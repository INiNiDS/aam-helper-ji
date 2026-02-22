package com.ininids.aamhelper.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.PsiTreeUtil

class AamImportStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getFilePath(): String? {
        val pathNode = node.findChildByType(AamTokenTypes.FILE_PATH)
        return pathNode?.text
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
}

class AamDeriveStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getFilePath(): String? {
        val pathNode = node.findChildByType(AamTokenTypes.FILE_PATH)
        return pathNode?.text
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
}

class AamSchemaField(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getFieldName(): String? = node.findChildByType(AamTokenTypes.FIELD_NAME)?.text
    fun getFieldType(): String? = node.findChildByType(AamTokenTypes.FIELD_TYPE)?.text
    override fun getName(): String? = getFieldName()
}

class AamSchemaDeclaration(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getSchemaName(): String? = node.findChildByType(AamTokenTypes.SCHEMA_NAME)?.text
    fun getFields(): List<AamSchemaField> =
        PsiTreeUtil.getChildrenOfTypeAsList(this, AamSchemaField::class.java)
    override fun getName(): String? = getSchemaName()
}

class AamProperty(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getKey(): String? {
        val keyNode = node.findChildByType(AamTokenTypes.KEY)
        return keyNode?.text
    }

    fun getValue(): String? {
        val valueNode = node.findChildByType(AamTokenTypes.VALUE)
        return valueNode?.text
    }

    override fun getName(): String? {
        return getKey()
    }
}

class AamTypeDeclaration(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getAliasName(): String? = node.findChildByType(AamTokenTypes.TYPE_ALIAS)?.text
    fun getBaseType(): String? = node.findChildByType(AamTokenTypes.TYPE_BASE)?.text
    override fun getName(): String? = getAliasName()
}

