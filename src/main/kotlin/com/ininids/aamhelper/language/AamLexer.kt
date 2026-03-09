package com.ininids.aamhelper.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AamLexer : LexerBase() {
    // Lexer states:
    //  0  — default / top-level
    //  1  — after '=' (value, inline object, or list)
    //  2  — after @import / @derive  (file path)
    //  3  — after @schema            (schema name → '{')
    //  4  — inside @schema body      (field name)
    //  5  — after field name         ('*' or ':')
    //  9  — after 'list'             ('<')
    //  10 — inside 'list<'           (inner type)
    //  11 — after 'list<T'           ('>')
    //  12 — inside inline object     ('{ k = v, … }')
    //  13 — inside list value        ('[ a, b, … ]')
    //  20 — after @derive file path  ('::SchemaName')
    //  55 — after ':' in field       (field type)
    //  6  — after @type              (alias name)
    //  7  — after @type alias        ('=')
    //  8  — after @type alias =      (base type)

    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentState: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null
    private var inlineBraceDepth: Int = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentState = initialState
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.inlineBraceDepth = 0
        advance()
    }

    override fun getState(): Int = currentState
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd

    private fun skipInlineWhitespace(): Boolean {
        val c = buffer[tokenStart]
        if (c != '\n' && Character.isWhitespace(c)) {
            tokenEnd++
            while (tokenEnd < endOffset && buffer[tokenEnd] != '\n' && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
            tokenType = TokenType.WHITE_SPACE
            return true
        }
        return false
    }

    private fun readIdentifier() {
        tokenEnd++
        while (tokenEnd < endOffset) {
            val c = buffer[tokenEnd]
            if (Character.isLetterOrDigit(c) || c == '_') tokenEnd++ else break
        }
    }

    private fun readNamespacedType() {
        tokenEnd++
        while (tokenEnd < endOffset) {
            val c = buffer[tokenEnd]
            when {
                Character.isLetterOrDigit(c) || c == '_' -> tokenEnd++
                c == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':' -> tokenEnd += 2
                else -> break
            }
        }
    }

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) { tokenType = null; return }

        val firstChar = buffer[tokenStart]

        // State 4: field name
        if (currentState == 4) {
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                tokenType = TokenType.WHITE_SPACE; return
            }
            when (firstChar) {
                '}' -> { tokenEnd++; tokenType = AamTokenTypes.RBRACE; currentState = 0; return }
                ',' -> { tokenEnd++; tokenType = AamTokenTypes.COMMA; return }
            }
            readIdentifier()
            tokenType = AamTokenTypes.FIELD_NAME
            currentState = 5
            return
        }

        // State 5: after field name — '*' or ':'
        if (currentState == 5) {
            if (skipInlineWhitespace()) return
            when (firstChar) {
                '*' -> { tokenEnd++; tokenType = AamTokenTypes.OPTIONAL_MARKER; return }
                ':' -> { tokenEnd++; tokenType = AamTokenTypes.COLON; currentState = 55; return }
                ',', '\n' -> { tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 4; return }
                '}' -> { tokenEnd++; tokenType = AamTokenTypes.RBRACE; currentState = 0; return }
                else -> { tokenEnd++; tokenType = TokenType.WHITE_SPACE; return }
            }
        }

        // State 55: after ':' — field type
        if (currentState == 55) {
            if (skipInlineWhitespace()) return
            when (firstChar) {
                '}' -> { tokenEnd++; tokenType = AamTokenTypes.RBRACE; currentState = 0; return }
                ',', '\n' -> { tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 4; return }
            }
            val remaining = buffer.subSequence(tokenStart, endOffset).toString()
            if (remaining.startsWith("list") &&
                (tokenStart + 4 >= endOffset || (!Character.isLetterOrDigit(buffer[tokenStart + 4]) && buffer[tokenStart + 4] != '_'))) {
                tokenEnd += 4
                tokenType = AamTokenTypes.LIST_KEYWORD
                currentState = 9
                return
            }
            readNamespacedType()
            tokenType = AamTokenTypes.FIELD_TYPE
            currentState = 4
            return
        }

        // State 9: after 'list' — '<'
        if (currentState == 9) {
            if (skipInlineWhitespace()) return
            if (firstChar == '<') { tokenEnd++; tokenType = AamTokenTypes.LANGLE; currentState = 10; return }
            tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 4; return
        }

        // State 10: inside 'list<' — inner type
        if (currentState == 10) {
            if (skipInlineWhitespace()) return
            readNamespacedType()
            tokenType = AamTokenTypes.FIELD_TYPE
            currentState = 11
            return
        }

        // State 11: after 'list<T' — '>'
        if (currentState == 11) {
            if (skipInlineWhitespace()) return
            if (firstChar == '>') { tokenEnd++; tokenType = AamTokenTypes.RANGLE; currentState = 4; return }
            tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 4; return
        }

        // State 3: @schema name → '{'
        if (currentState == 3) {
            if (skipInlineWhitespace()) return
            when (firstChar) {
                '\n' -> { tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 0; return }
                '{' -> { tokenEnd++; tokenType = AamTokenTypes.LBRACE; currentState = 4; return }
                else -> { readIdentifier(); tokenType = AamTokenTypes.SCHEMA_NAME; return }
            }
        }

        // State 2: file path (for @import / @derive)
        if (currentState == 2) {
            when {
                firstChar == '\n' || firstChar == '#' -> currentState = 0
                Character.isWhitespace(firstChar) -> {
                    tokenEnd++
                    while (tokenEnd < endOffset && buffer[tokenEnd] != '\n' && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                    tokenType = TokenType.WHITE_SPACE; return
                }
                else -> {
                    while (tokenEnd < endOffset) {
                        val c = buffer[tokenEnd]
                        if (Character.isWhitespace(c) || c == '#') break
                        if (c == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':') break
                        tokenEnd++
                    }
                    if (tokenEnd > tokenStart) {
                        tokenType = AamTokenTypes.FILE_PATH
                        currentState = if (tokenEnd < endOffset &&
                            buffer[tokenEnd] == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':') 20 else 0
                        return
                    }
                }
            }
        }

        // State 20: '::SchemaName' after @derive file path
        if (currentState == 20) {
            if (firstChar == ':' && tokenEnd + 1 < endOffset && buffer[tokenEnd + 1] == ':') {
                tokenEnd += 2
                readIdentifier()
                tokenType = AamTokenTypes.DERIVE_SCHEMA
                currentState = 0
                return
            }
            currentState = 0
        }

        // State 1: value after '='
        if (currentState == 1) {
            when {
                firstChar == '\n' || firstChar == '#' -> currentState = 0
                Character.isWhitespace(firstChar) -> {
                    tokenEnd++
                    while (tokenEnd < endOffset && buffer[tokenEnd] != '\n' && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                    tokenType = TokenType.WHITE_SPACE; return
                }
                firstChar == '{' -> {
                    tokenEnd++; tokenType = AamTokenTypes.LBRACE
                    inlineBraceDepth = 1; currentState = 12; return
                }
                firstChar == '[' -> {
                    tokenEnd++; tokenType = AamTokenTypes.LBRACKET; currentState = 13; return
                }
                else -> {
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
                    if (tokenEnd > tokenStart) { tokenType = AamTokenTypes.VALUE; currentState = 0; return }
                }
            }
        }

        // State 12: inline object body
        if (currentState == 12) {
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                tokenType = TokenType.WHITE_SPACE; return
            }
            when (firstChar) {
                '}' -> {
                    tokenEnd++; tokenType = AamTokenTypes.RBRACE
                    inlineBraceDepth--
                    if (inlineBraceDepth <= 0) { inlineBraceDepth = 0; currentState = 0 }
                    return
                }
                '{' -> { tokenEnd++; tokenType = AamTokenTypes.LBRACE; inlineBraceDepth++; return }
                '=' -> { tokenEnd++; tokenType = AamTokenTypes.EQUALS; return }
                ',' -> { tokenEnd++; tokenType = AamTokenTypes.COMMA; return }
                '#' -> {
                    tokenEnd++
                    while (tokenEnd < endOffset && buffer[tokenEnd] != '\n') tokenEnd++
                    tokenType = AamTokenTypes.COMMENT; return
                }
                else -> {
                    tokenEnd++
                    while (tokenEnd < endOffset) {
                        val c = buffer[tokenEnd]
                        if (c == ',' || c == '}' || c == '{' || c == '=' || c == '\n' || c == '#' || Character.isWhitespace(c)) break
                        tokenEnd++
                    }
                    tokenType = AamTokenTypes.KEY; return
                }
            }
        }

        // State 13: list value body
        if (currentState == 13) {
            if (Character.isWhitespace(firstChar)) {
                tokenEnd++
                while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                tokenType = TokenType.WHITE_SPACE; return
            }
            when (firstChar) {
                ']' -> { tokenEnd++; tokenType = AamTokenTypes.RBRACKET; currentState = 0; return }
                ',' -> { tokenEnd++; tokenType = AamTokenTypes.COMMA; return }
                '#' -> {
                    tokenEnd++
                    while (tokenEnd < endOffset && buffer[tokenEnd] != '\n') tokenEnd++
                    tokenType = AamTokenTypes.COMMENT; return
                }
                else -> {
                    var inQuote = false
                    var quoteChar = '\u0000'
                    while (tokenEnd < endOffset) {
                        val c = buffer[tokenEnd]
                        if (c == ']' || c == ',' || c == '\n') break
                        if (c == '"' || c == '\'') {
                            if (!inQuote) { inQuote = true; quoteChar = c }
                            else if (c == quoteChar) { tokenEnd++; break }
                        }
                        if (c == '#' && !inQuote) break
                        tokenEnd++
                    }
                    if (tokenEnd > tokenStart) { tokenType = AamTokenTypes.VALUE; return }
                    tokenEnd++; tokenType = TokenType.WHITE_SPACE; return
                }
            }
        }

        // State 6: @type alias name
        if (currentState == 6) {
            if (firstChar == '\n') { tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 0; return }
            if (skipInlineWhitespace()) return
            readIdentifier(); tokenType = AamTokenTypes.TYPE_ALIAS; currentState = 7; return
        }

        // State 7: '=' after @type alias
        if (currentState == 7) {
            if (firstChar == '\n') { tokenEnd++; tokenType = TokenType.WHITE_SPACE; currentState = 0; return }
            if (skipInlineWhitespace()) return
            if (firstChar == '=') { tokenEnd++; tokenType = AamTokenTypes.TYPE_EQUALS; currentState = 8; return }
            tokenEnd++; tokenType = TokenType.WHITE_SPACE; return
        }

        // State 8: base type after @type alias =
        if (currentState == 8) {
            when {
                firstChar == '\n' || firstChar == '#' -> currentState = 0
                Character.isWhitespace(firstChar) -> {
                    tokenEnd++
                    while (tokenEnd < endOffset && buffer[tokenEnd] != '\n' && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
                    tokenType = TokenType.WHITE_SPACE; return
                }
                else -> {
                    readNamespacedType()
                    if (tokenEnd > tokenStart) { tokenType = AamTokenTypes.TYPE_BASE; currentState = 0; return }
                }
            }
        }

        // State 0: default
        if (Character.isWhitespace(firstChar)) {
            tokenEnd++
            while (tokenEnd < endOffset && Character.isWhitespace(buffer[tokenEnd])) tokenEnd++
            tokenType = TokenType.WHITE_SPACE; return
        }
        if (firstChar == '#') {
            tokenEnd++
            while (tokenEnd < endOffset && buffer[tokenEnd] != '\n') tokenEnd++
            tokenType = AamTokenTypes.COMMENT; return
        }
        if (firstChar == '@') {
            val rest = buffer.subSequence(tokenStart, endOffset).toString()
            when {
                rest.startsWith("@import") -> { tokenEnd += 7; tokenType = AamTokenTypes.IMPORT_KEYWORD; currentState = 2; return }
                rest.startsWith("@derive") -> { tokenEnd += 7; tokenType = AamTokenTypes.DERIVE_KEYWORD; currentState = 2; return }
                rest.startsWith("@schema") -> { tokenEnd += 7; tokenType = AamTokenTypes.SCHEMA_KEYWORD; currentState = 3; return }
                rest.startsWith("@type")   -> { tokenEnd += 5; tokenType = AamTokenTypes.TYPE_KEYWORD;   currentState = 6; return }
            }
        }
        if (firstChar == '=') { tokenEnd++; tokenType = AamTokenTypes.EQUALS; currentState = 1; return }

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