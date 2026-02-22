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
                    builder.advanceLexer()
                    if (builder.tokenType == AamTokenTypes.EQUALS) {
                        builder.advanceLexer()
                    }
                    if (builder.tokenType == AamTokenTypes.VALUE) {
                        builder.advanceLexer()
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
                        builder.advanceLexer() // consume '{'
                        // parse fields
                        while (!builder.eof() && builder.tokenType != AamTokenTypes.RBRACE) {
                            if (builder.tokenType == AamTokenTypes.FIELD_NAME) {
                                val fieldMarker = builder.mark()
                                builder.advanceLexer() // FIELD_NAME
                                if (builder.tokenType == AamTokenTypes.COLON) {
                                    builder.advanceLexer() // COLON
                                }
                                if (builder.tokenType == AamTokenTypes.FIELD_TYPE) {
                                    builder.advanceLexer() // FIELD_TYPE
                                }
                                fieldMarker.done(AamElementTypes.SCHEMA_FIELD)
                                if (builder.tokenType == AamTokenTypes.COMMA) {
                                    builder.advanceLexer() // COMMA
                                }
                            } else {
                                builder.advanceLexer()
                            }
                        }
                        if (builder.tokenType == AamTokenTypes.RBRACE) {
                            builder.advanceLexer() // consume '}'
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
                    builder.advanceLexer() // consume @type
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
}
