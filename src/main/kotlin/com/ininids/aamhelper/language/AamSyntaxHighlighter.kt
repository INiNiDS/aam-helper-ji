package com.ininids.aamhelper.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AamSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val KEY = createTextAttributesKey("AAM_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE = createTextAttributesKey("AAM_VALUE", DefaultLanguageHighlighterColors.STRING)
        val COMMENT = createTextAttributesKey("AAM_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val DIRECTIVE = createTextAttributesKey("AAM_DIRECTIVE", DefaultLanguageHighlighterColors.METADATA)
        val FILE_PATH_KEY = createTextAttributesKey("AAM_FILE_PATH", DefaultLanguageHighlighterColors.STRING)
        val SCHEMA_NAME_KEY = createTextAttributesKey("AAM_SCHEMA_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
        val FIELD_NAME_KEY = createTextAttributesKey("AAM_FIELD_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val FIELD_TYPE_KEY = createTextAttributesKey("AAM_FIELD_TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val BRACE_KEY = createTextAttributesKey("AAM_BRACE", DefaultLanguageHighlighterColors.BRACES)
        val COLON_KEY = createTextAttributesKey("AAM_COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val COMMA_KEY = createTextAttributesKey("AAM_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val EQUALS_KEY = createTextAttributesKey("AAM_EQUALS", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val BAD_CHARACTER = createTextAttributesKey("AAM_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        val TYPE_ALIAS_KEY = createTextAttributesKey("AAM_TYPE_ALIAS", DefaultLanguageHighlighterColors.CLASS_NAME)
        val TYPE_BASE_KEY = createTextAttributesKey("AAM_TYPE_BASE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)

        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val TYPE_ALIAS_KEYS = arrayOf(TYPE_ALIAS_KEY)
        private val TYPE_BASE_KEYS = arrayOf(TYPE_BASE_KEY)
        private val KEY_KEYS = arrayOf(KEY)
        private val VALUE_KEYS = arrayOf(VALUE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val DIRECTIVE_KEYS = arrayOf(DIRECTIVE)
        private val FILE_PATH_KEYS = arrayOf(FILE_PATH_KEY)
        private val SCHEMA_NAME_KEYS = arrayOf(SCHEMA_NAME_KEY)
        private val FIELD_NAME_KEYS = arrayOf(FIELD_NAME_KEY)
        private val FIELD_TYPE_KEYS = arrayOf(FIELD_TYPE_KEY)
        private val BRACE_KEYS = arrayOf(BRACE_KEY)
        private val COLON_KEYS = arrayOf(COLON_KEY)
        private val COMMA_KEYS = arrayOf(COMMA_KEY)
        private val EQUALS_KEYS = arrayOf(EQUALS_KEY)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = AamLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AamTokenTypes.KEY -> KEY_KEYS
            AamTokenTypes.VALUE -> VALUE_KEYS
            AamTokenTypes.COMMENT -> COMMENT_KEYS
            AamTokenTypes.IMPORT_KEYWORD,
            AamTokenTypes.DERIVE_KEYWORD,
            AamTokenTypes.SCHEMA_KEYWORD,
            AamTokenTypes.TYPE_KEYWORD -> DIRECTIVE_KEYS
            AamTokenTypes.TYPE_ALIAS -> TYPE_ALIAS_KEYS
            AamTokenTypes.TYPE_BASE -> TYPE_BASE_KEYS
            AamTokenTypes.TYPE_EQUALS -> COLON_KEYS
            AamTokenTypes.FILE_PATH -> FILE_PATH_KEYS
            AamTokenTypes.SCHEMA_NAME -> SCHEMA_NAME_KEYS
            AamTokenTypes.FIELD_NAME -> FIELD_NAME_KEYS
            AamTokenTypes.FIELD_TYPE -> FIELD_TYPE_KEYS
            AamTokenTypes.LBRACE, AamTokenTypes.RBRACE -> BRACE_KEYS
            AamTokenTypes.COLON -> COLON_KEYS
            AamTokenTypes.COMMA -> COMMA_KEYS
            AamTokenTypes.EQUALS -> EQUALS_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }
    }
}
