package com.ininids.aamhelper.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
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

    /** Returns the schema name from `@derive file.aam::SchemaName`, or null if not selective. */
    fun getSelectiveSchema(): String? {
        val schemaNode = node.findChildByType(AamTokenTypes.DERIVE_SCHEMA)
        // Lexer emits "::SchemaName" as the token text — strip the leading "::"
        return schemaNode?.text?.removePrefix("::")
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
}

class AamSchemaField(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getFieldName(): String? = node.findChildByType(AamTokenTypes.FIELD_NAME)?.text
    /** Returns true if this field is declared optional (with `*`). */
    fun isOptional(): Boolean = node.findChildByType(AamTokenTypes.OPTIONAL_MARKER) != null
    /** Returns true if this field type is `list<T>`. */
    fun isListType(): Boolean = node.findChildByType(AamTokenTypes.LIST_KEYWORD) != null
    /** Returns the raw type text: the inner type for lists, or the direct FIELD_TYPE for plain fields. */
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
    fun getKey(): String? = node.findChildByType(AamTokenTypes.KEY)?.text
    fun getValue(): String? = node.findChildByType(AamTokenTypes.VALUE)?.text
    @Suppress("unused") fun getInlineValue(): AamInlineValue? = PsiTreeUtil.getChildOfType(this, AamInlineValue::class.java)
    @Suppress("unused") fun getListValue(): AamListValue? = PsiTreeUtil.getChildOfType(this, AamListValue::class.java)
    override fun getName(): String? = getKey()
}

class AamInlineValue(node: ASTNode) : ASTWrapperPsiElement(node) {
    @Suppress("unused")
    fun getKeys(): List<String> {
        val keys = mutableListOf<String>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == AamTokenTypes.KEY) keys.add(child.text)
            child = child.treeNext
        }
        return keys
    }

    /**
     * Returns pairs of (key, nested AamInlineValue or null) for each direct key in this inline object.
     * Used for recursive schema completeness checks.
     */
    fun getProperties(): List<Pair<String, AamInlineValue?>> {
        val result = mutableListOf<Pair<String, AamInlineValue?>>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == AamTokenTypes.KEY) {
                val key = child.text
                // look ahead for a nested INLINE_VALUE sibling
                var next = child.treeNext
                var nested: AamInlineValue? = null
                while (next != null) {
                    val type = next.elementType
                    if (type == AamElementTypes.INLINE_VALUE) {
                        nested = next.psi as? AamInlineValue
                        break
                    }
                    if (type == AamTokenTypes.KEY || type == AamTokenTypes.COMMA) break
                    next = next.treeNext
                }
                result.add(key to nested)
            }
            child = child.treeNext
        }
        return result
    }
}

class AamListValue(node: ASTNode) : ASTWrapperPsiElement(node) {
    @Suppress("unused")
    fun getItems(): List<String> {
        val items = mutableListOf<String>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == AamTokenTypes.VALUE) items.add(child.text.trim())
            child = child.treeNext
        }
        return items
    }
}

class AamTypeDeclaration(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun getAliasName(): String? = node.findChildByType(AamTokenTypes.TYPE_ALIAS)?.text
    fun getBaseType(): String? = node.findChildByType(AamTokenTypes.TYPE_BASE)?.text
    override fun getName(): String? = getAliasName()
}
