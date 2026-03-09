package com.ininids.aamhelper.language

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class AamParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        while (!builder.eof()) {
            val tokenType = builder.tokenType
            when (tokenType) {
                AamTokenTypes.KEY -> {
                    val propertyMarker = builder.mark()
                    builder.advanceLexer() // KEY
                    if (builder.tokenType == AamTokenTypes.EQUALS) {
                        builder.advanceLexer() // EQUALS
                    }
                    when (builder.tokenType) {
                        AamTokenTypes.LBRACE -> {
                            parseInlineValue(builder)
                        }
                        AamTokenTypes.LBRACKET -> {
                            parseListValue(builder)
                        }
                        AamTokenTypes.VALUE -> {
                            builder.advanceLexer()
                        }
                        else -> { /* empty value */ }
                    }
                    propertyMarker.done(AamElementTypes.PROPERTY)
                }
                AamTokenTypes.IMPORT_KEYWORD -> {
                    val importMarker = builder.mark()
                    builder.advanceLexer()
                    if (builder.tokenType == AamTokenTypes.FILE_PATH) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected file path after @import")
                    }
                    importMarker.done(AamElementTypes.IMPORT_STATEMENT)
                }
                AamTokenTypes.DERIVE_KEYWORD -> {
                    val deriveMarker = builder.mark()
                    builder.advanceLexer()
                    if (builder.tokenType == AamTokenTypes.FILE_PATH) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected file path after @derive")
                    }
                    if (builder.tokenType == AamTokenTypes.DERIVE_SCHEMA) {
                        builder.advanceLexer()
                    }
                    deriveMarker.done(AamElementTypes.DERIVE_STATEMENT)
                }
                AamTokenTypes.SCHEMA_KEYWORD -> {
                    val schemaMarker = builder.mark()
                    builder.advanceLexer() // consume @schema
                    if (builder.tokenType == AamTokenTypes.SCHEMA_NAME) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected schema name after @schema")
                    }
                    if (builder.tokenType == AamTokenTypes.LBRACE) {
                        builder.advanceLexer()
                        while (!builder.eof() && builder.tokenType != AamTokenTypes.RBRACE) {
                            if (builder.tokenType == AamTokenTypes.FIELD_NAME) {
                                val fieldMarker = builder.mark()
                                builder.advanceLexer()
                                if (builder.tokenType == AamTokenTypes.OPTIONAL_MARKER) {
                                    builder.advanceLexer()
                                }
                                if (builder.tokenType == AamTokenTypes.COLON) {
                                    builder.advanceLexer()
                                }
                                if (builder.tokenType == AamTokenTypes.LIST_KEYWORD) {
                                    builder.advanceLexer()
                                    if (builder.tokenType == AamTokenTypes.LANGLE) {
                                        builder.advanceLexer()
                                    }
                                    if (builder.tokenType == AamTokenTypes.FIELD_TYPE) {
                                        builder.advanceLexer()
                                    }
                                    if (builder.tokenType == AamTokenTypes.RANGLE) {
                                        builder.advanceLexer()
                                    }
                                } else if (builder.tokenType == AamTokenTypes.FIELD_TYPE) {
                                    builder.advanceLexer()
                                }
                                fieldMarker.done(AamElementTypes.SCHEMA_FIELD)
                                if (builder.tokenType == AamTokenTypes.COMMA) {
                                    builder.advanceLexer()
                                }
                            } else {
                                builder.advanceLexer()
                            }
                        }
                        if (builder.tokenType == AamTokenTypes.RBRACE) {
                            builder.advanceLexer()
                        } else {
                            builder.error("Expected '}' to close schema")
                        }
                    } else {
                        builder.error("Expected '{' after schema name")
                    }
                    schemaMarker.done(AamElementTypes.SCHEMA_DECLARATION)
                }
                AamTokenTypes.COMMENT -> builder.advanceLexer()
                AamTokenTypes.TYPE_KEYWORD -> {
                    val typeMarker = builder.mark()
                    builder.advanceLexer()
                    if (builder.tokenType == AamTokenTypes.TYPE_ALIAS) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected alias name after @type")
                    }
                    if (builder.tokenType == AamTokenTypes.TYPE_EQUALS) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected '=' after type alias name")
                    }
                    if (builder.tokenType == AamTokenTypes.TYPE_BASE) {
                        builder.advanceLexer()
                    } else {
                        builder.error("Expected base type after '='")
                    }
                    typeMarker.done(AamElementTypes.TYPE_DECLARATION)
                }
                else -> builder.advanceLexer()
            }
        }
        rootMarker.done(root)
        return builder.treeBuilt
    }

    /**
     * Parses an inline object value: `{ key = value, key2 = { ... }, ... }`
     * The opening LBRACE has NOT been consumed yet.
     */
    private fun parseInlineValue(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume '{'
        var depth = 1
        while (!builder.eof() && depth > 0) {
            when (builder.tokenType) {
                AamTokenTypes.LBRACE -> {
                    parseInlineValue(builder)
                }
                AamTokenTypes.RBRACE -> {
                    depth--
                    builder.advanceLexer()
                }
                AamTokenTypes.KEY -> {
                    builder.advanceLexer() // key
                    if (builder.tokenType == AamTokenTypes.EQUALS) {
                        builder.advanceLexer()
                    }
                    // value could be scalar KEY/VALUE token or a nested inline object
                    when (builder.tokenType) {
                        AamTokenTypes.LBRACE -> parseInlineValue(builder)
                        AamTokenTypes.KEY, AamTokenTypes.VALUE -> builder.advanceLexer()
                        else -> { /* nothing */ }
                    }
                }
                AamTokenTypes.COMMA -> builder.advanceLexer()
                AamTokenTypes.COMMENT -> builder.advanceLexer()
                else -> builder.advanceLexer()
            }
        }
        marker.done(AamElementTypes.INLINE_VALUE)
    }

    /**
     * Parses a list value: `[ item1, item2, item3 ]`
     * The opening LBRACKET has NOT been consumed yet.
     */
    private fun parseListValue(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume '['
        while (!builder.eof() && builder.tokenType != AamTokenTypes.RBRACKET) {
            when (builder.tokenType) {
                AamTokenTypes.VALUE, AamTokenTypes.KEY -> builder.advanceLexer()
                AamTokenTypes.COMMA -> builder.advanceLexer()
                AamTokenTypes.COMMENT -> builder.advanceLexer()
                else -> builder.advanceLexer()
            }
        }
        if (builder.tokenType == AamTokenTypes.RBRACKET) {
            builder.advanceLexer()
        } else {
            builder.error("Expected ']' to close list")
        }
        marker.done(AamElementTypes.LIST_VALUE)
    }
}
