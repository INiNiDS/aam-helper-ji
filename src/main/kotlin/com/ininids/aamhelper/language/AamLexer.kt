package com.ininids.aamhelper.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AamLexer : LexerBase() {
    // States:
    // 0 = default
    // 1 = after '=' (read value)
    // 2 = after @import or @derive (read file path)
    // 3 = after @schema (read schema name then '{')
    // 4 = inside @schema body { field: type, ... }
    // 5 = inside @schema body, expecting field type
    // 6 = after @type keyword (read alias name)
    // 7 = after @type alias (read '=')
    // 8 = after @type alias = (read base type)

    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentState: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentState = initialState
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = currentState
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            tokenType = null
            return
        }

        val firstChar = buffer[tokenStart]

        // State 4: inside @schema body { field: type, ... }
        if (currentState == 4) {
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (firstChar == '}') {
                tokenEnd++
                tokenType = AamTokenTypes.RBRACE
                currentState = 0
                return
            }
            if (firstChar == ':') {
                tokenEnd++
                tokenType = AamTokenTypes.COLON
                return
            }
            if (firstChar == ',') {
                tokenEnd++
                tokenType = AamTokenTypes.COMMA
                return
            }
            // Check if we already emitted a FIELD_NAME for this pair
            // Heuristic: if previous non-ws token was COLON, this is FIELD_TYPE; otherwise FIELD_NAME
            // We track via a flag embedded in state: 40 = expecting field name, 41 = expecting field type
            // Actually, we use state 4 = expecting field name, 5 = expecting field type
            if (currentState == 4) {
                // read identifier as FIELD_NAME
                tokenEnd++
                while (tokenEnd < endOffset) {
                    val c = buffer[tokenEnd]
                    if (Character.isLetterOrDigit(c) || c == '_') tokenEnd++ else break
                }
                tokenType = AamTokenTypes.FIELD_NAME
                currentState = 5
                return
            }
        }

        // State 5: expecting field type
        if (currentState == 5) {
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (firstChar == ':') {
                tokenEnd++
                tokenType = AamTokenTypes.COLON
                return
            }
            if (firstChar == ',') {
                tokenEnd++
                tokenType = AamTokenTypes.COMMA
                currentState = 4
                return
            }
            if (firstChar == '}') {
                tokenEnd++
                tokenType = AamTokenTypes.RBRACE
                currentState = 0
                return
            }
            // read identifier as FIELD_TYPE (supports namespaced types like math::vector2)
            tokenEnd++
            while (tokenEnd < endOffset) {
                val c = buffer[tokenEnd]
                if (Character.isLetterOrDigit(c) || c == '_') {
                    tokenEnd++
                } else if (c == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':') {
                    tokenEnd += 2
                } else {
                    break
                }
            }
            tokenType = AamTokenTypes.FIELD_TYPE
            currentState = 4
            return
        }

        // State 3: after @schema keyword — read schema name, then expect '{'
        if (currentState == 3) {
            if (Character.isWhitespace(firstChar) && firstChar != '\n') {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd]) && buffer[tokenEnd] != '\n') tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (firstChar == '\n') {
                currentState = 0
                tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (firstChar == '{') {
                tokenEnd++
                tokenType = AamTokenTypes.LBRACE
                currentState = 4
                return
            }
            // Read schema name identifier
            tokenEnd++
            while (tokenEnd < endOffset) {
                val c = buffer[tokenEnd]
                if (Character.isLetterOrDigit(c) || c == '_') tokenEnd++ else break
            }
            tokenType = AamTokenTypes.SCHEMA_NAME
            // Stay in state 3 to then read '{'
            return
        }

        // State 2: after @import or @derive — read file path
        if (currentState == 2) {
            if (firstChar == '\n' || firstChar == '#') {
                currentState = 0
            } else if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd]) && buffer[tokenEnd] != '\n') tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            } else {
                while (tokenEnd < endOffset) {
                    val c = buffer[tokenEnd]
                    if (Character.isWhitespace(c) || c == '#') break
                    tokenEnd++
                }
                if (tokenEnd > tokenStart) {
                    tokenType = AamTokenTypes.FILE_PATH
                    currentState = 0
                    return
                }
            }
        }

        // State 1: after '=' — read value
        if (currentState == 1) {
            if (firstChar == '\n' || firstChar == '#') {
                currentState = 0
            } else {
                var inQuote = false
                var quoteChar = '\u0000'
                while (tokenEnd < endOffset) {
                    val c = buffer[tokenEnd]
                    if (c == '\n') break
                    if (c == '"' || c == '\'') {
                        if (!inQuote) { inQuote = true; quoteChar = c }
                        else if (c == quoteChar) inQuote = false
                    }
                    if (c == '#' && !inQuote) break
                    tokenEnd++
                }
                if (tokenEnd > tokenStart) {
                    tokenType = AamTokenTypes.VALUE
                    currentState = 0
                    return
                }
            }
        }

        // State 6: after @type — read alias name
        if (currentState == 6) {
            if (firstChar == '\n') {
                currentState = 0
                tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd]) && buffer[tokenEnd] != '\n') tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            // read alias identifier
            tokenEnd++
            while (tokenEnd < endOffset) {
                val c = buffer[tokenEnd]
                if (Character.isLetterOrDigit(c) || c == '_') tokenEnd++ else break
            }
            tokenType = AamTokenTypes.TYPE_ALIAS
            currentState = 7
            return
        }

        // State 7: after @type alias — read '='
        if (currentState == 7) {
            if (firstChar == '\n') {
                currentState = 0
                tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd]) && buffer[tokenEnd] != '\n') tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            }
            if (firstChar == '=') {
                tokenEnd++
                tokenType = AamTokenTypes.TYPE_EQUALS
                currentState = 8
                return
            }
            // unexpected char
            tokenEnd++
            tokenType = TokenType.WHITE_SPACE
            return
        }

        // State 8: after @type alias = — read base type
        if (currentState == 8) {
            if (firstChar == '\n' || firstChar == '#') {
                currentState = 0
            } else if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd]) && buffer[tokenEnd] != '\n') tokenEnd++
                tokenType = TokenType.WHITE_SPACE
                return
            } else {
                tokenEnd++
                while (tokenEnd < endOffset) {
                    val c = buffer[tokenEnd]
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        tokenEnd++
                    } else if (c == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':') {
                        tokenEnd += 2
                    } else {
                        break
                    }
                }
                if (tokenEnd > tokenStart) {
                    tokenType = AamTokenTypes.TYPE_BASE
                    currentState = 0
                    return
                }
            }
        }

        // Default state 0

        if (Character.isWhitespace(firstChar)) {
            tokenEnd++
            while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
            tokenType = TokenType.WHITE_SPACE
            return
        }

        if (firstChar == '#') {
            tokenEnd++
            while (tokenEnd < endOffset && buffer[tokenEnd] != '\n') tokenEnd++
            tokenType = AamTokenTypes.COMMENT
            return
        }

        if (firstChar == '@') {
            val remaining = buffer.subSequence(tokenStart, endOffset).toString()
            if (remaining.startsWith("@import")) {
                tokenEnd += 7
                tokenType = AamTokenTypes.IMPORT_KEYWORD
                currentState = 2
                return
            }
            if (remaining.startsWith("@derive")) {
                tokenEnd += 7
                tokenType = AamTokenTypes.DERIVE_KEYWORD
                currentState = 2
                return
            }
            if (remaining.startsWith("@schema")) {
                tokenEnd += 7
                tokenType = AamTokenTypes.SCHEMA_KEYWORD
                currentState = 3
                return
            }
            if (remaining.startsWith("@type")) {
                tokenEnd += 5
                tokenType = AamTokenTypes.TYPE_KEYWORD
                currentState = 6
                return
            }
            // Unknown @ keyword — consume as key
        }

        if (firstChar == '=') {
            tokenEnd++
            currentState = 1
            tokenType = TokenType.WHITE_SPACE
            return
        }

        while (tokenEnd < endOffset) {
            val c = buffer[tokenEnd]
            if (Character.isWhitespace(c) || c == '=' || c == '#') break
            tokenEnd++
        }
        tokenType = AamTokenTypes.KEY
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset
}