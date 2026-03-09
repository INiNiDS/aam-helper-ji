package com.ininids.aamhelper.language

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * Handles "Go to Declaration" (Ctrl+B / Cmd+B) for:
 *
 *  1. DERIVE_SCHEMA token  (`::SchemaName`)  → jumps to the @schema declaration
 *     in the referenced file.
 *  2. FILE_PATH token inside @import / @derive → jumps to the target .aam file
 *     (already handled via PsiReference, this is a fallback).
 *  3. FIELD_TYPE token → if the type is a schema name defined in the current file,
 *     jumps to that @schema declaration.
 */
class AamGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val tokenType = element.node?.elementType ?: return null

        return when (tokenType) {
            // ── ::SchemaName after @derive file.aam ─────────────────────────────
            AamTokenTypes.DERIVE_SCHEMA -> resolveSchemaFromDeriveToken(element)

            // ── FILE_PATH inside @import or @derive ──────────────────────────────
            AamTokenTypes.FILE_PATH -> resolveFilePath(element)

            // ── FIELD_TYPE → could be a @schema name ────────────────────────────
            AamTokenTypes.FIELD_TYPE -> resolveFieldType(element)

            // ── SCHEMA_NAME token itself → navigate to the declaration ────────────
            AamTokenTypes.SCHEMA_NAME -> resolveSchemaName(element)

            else -> null
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    /** Resolve `::SchemaName` token: find the @schema in the file pointed to by the @derive. */
    private fun resolveSchemaFromDeriveToken(element: PsiElement): Array<PsiElement>? {
        val deriveStatement = element.parent as? AamDeriveStatement ?: return null
        val schemaName = deriveStatement.getSelectiveSchema() ?: return null
        val pathText = deriveStatement.getFilePath() ?: return null

        val currentDir = element.containingFile?.virtualFile?.parent ?: return null
        val targetVFile = currentDir.findFileByRelativePath(pathText) ?: return null
        val targetPsi = PsiManager.getInstance(element.project).findFile(targetVFile) ?: return null

        val schema = PsiTreeUtil.getChildrenOfTypeAsList(targetPsi, AamSchemaDeclaration::class.java)
            .firstOrNull { it.getSchemaName() == schemaName } ?: return null
        return arrayOf(schema)
    }

    /** Resolve FILE_PATH: jump to the referenced .aam file. */
    private fun resolveFilePath(element: PsiElement): Array<PsiElement>? {
        val pathText = element.text ?: return null
        val currentDir = element.containingFile?.virtualFile?.parent ?: return null
        val targetVFile = currentDir.findFileByRelativePath(pathText) ?: return null
        val targetPsi = PsiManager.getInstance(element.project).findFile(targetVFile) ?: return null
        return arrayOf(targetPsi)
    }

    /** Resolve a FIELD_TYPE that matches a @schema name in this or a derived file. */
    private fun resolveFieldType(element: PsiElement): Array<PsiElement>? {
        val typeName = element.text ?: return null
        val file = element.containingFile ?: return null

        // Look in the current file first
        val localSchema = PsiTreeUtil.getChildrenOfTypeAsList(file, AamSchemaDeclaration::class.java)
            .firstOrNull { it.getSchemaName() == typeName }
        if (localSchema != null) return arrayOf(localSchema)
        return null
    }

    /** Resolve a SCHEMA_NAME token to the enclosing AamSchemaDeclaration. */
    private fun resolveSchemaName(element: PsiElement): Array<PsiElement>? {
        val decl = element.parent as? AamSchemaDeclaration ?: return null
        return arrayOf(decl)
    }
}

