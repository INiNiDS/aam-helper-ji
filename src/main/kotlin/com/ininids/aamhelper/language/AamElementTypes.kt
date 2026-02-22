package com.ininids.aamhelper.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

object AamElementTypes {
    val PROPERTY = AamElementType("PROPERTY")
    val IMPORT_STATEMENT = AamElementType("IMPORT_STATEMENT")
    val DERIVE_STATEMENT = AamElementType("DERIVE_STATEMENT")
    val SCHEMA_DECLARATION = AamElementType("SCHEMA_DECLARATION")
    val SCHEMA_FIELD = AamElementType("SCHEMA_FIELD")
    val TYPE_DECLARATION = AamElementType("TYPE_DECLARATION")

    object Factory {
        fun createElement(node: ASTNode): PsiElement {
            return when (node.elementType) {
                PROPERTY -> AamProperty(node)
                IMPORT_STATEMENT -> AamImportStatement(node)
                DERIVE_STATEMENT -> AamDeriveStatement(node)
                SCHEMA_DECLARATION -> AamSchemaDeclaration(node)
                SCHEMA_FIELD -> AamSchemaField(node)
                TYPE_DECLARATION -> AamTypeDeclaration(node)
                else -> ASTWrapperPsiElement(node)
            }
        }
    }
}


