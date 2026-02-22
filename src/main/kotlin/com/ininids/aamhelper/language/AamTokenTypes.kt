package com.ininids.aamhelper.language

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class AamTokenType(@NonNls debugName: String) : IElementType(debugName, AamLanguage) {
    override fun toString(): String {
        return "AamTokenType." + super.toString()
    }
}

class AamElementType(@NonNls debugName: String) : IElementType(debugName, AamLanguage)

object AamTokenTypes {
    val KEY = AamTokenType("KEY")
    val VALUE = AamTokenType("VALUE")
    val EQUALS = AamTokenType("EQUALS")
    val COMMENT = AamTokenType("COMMENT")
    val IMPORT_KEYWORD = AamTokenType("IMPORT_KEYWORD")
    val DERIVE_KEYWORD = AamTokenType("DERIVE_KEYWORD")
    val SCHEMA_KEYWORD = AamTokenType("SCHEMA_KEYWORD")
    val TYPE_KEYWORD = AamTokenType("TYPE_KEYWORD")
    val FILE_PATH = AamTokenType("FILE_PATH")
    val SCHEMA_NAME = AamTokenType("SCHEMA_NAME")
    val LBRACE = AamTokenType("LBRACE")
    val RBRACE = AamTokenType("RBRACE")
    val FIELD_NAME = AamTokenType("FIELD_NAME")
    val FIELD_TYPE = AamTokenType("FIELD_TYPE")
    val COLON = AamTokenType("COLON")
    val COMMA = AamTokenType("COMMA")
    val TYPE_ALIAS = AamTokenType("TYPE_ALIAS")
    val TYPE_EQUALS = AamTokenType("TYPE_EQUALS")
    val TYPE_BASE = AamTokenType("TYPE_BASE")
}
