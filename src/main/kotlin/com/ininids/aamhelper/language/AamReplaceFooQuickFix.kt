package com.ininids.aamhelper.language

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Placeholder quick-fix. Can be extended to offer real code actions for AAM files.
 */
class AamReplaceFooQuickFix(private val element: PsiElement) : BaseIntentionAction() {

    override fun getFamilyName(): String = "AAM fixes"

    override fun getText(): String = "AAM: placeholder fix"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = false

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        // nothing to do
    }
}