package com.ininids.aamhelper.language

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import javax.swing.Icon

class AamBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages(): Array<Language> = arrayOf(AamLanguage)

    override fun acceptElement(element: PsiElement): Boolean {
        return element is AamProperty
            || element is AamImportStatement
            || element is AamDeriveStatement
            || element is AamSchemaDeclaration
    }

    override fun getElementInfo(element: PsiElement): String {
        return when (element) {
            is AamProperty -> element.getKey() ?: "Property"
            is AamImportStatement -> "@import ${element.getFilePath() ?: ""}"
            is AamDeriveStatement -> "@derive ${element.getFilePath() ?: ""}"
            is AamSchemaDeclaration -> "@schema ${element.getSchemaName() ?: ""}"
            else -> "..."
        }
    }

    override fun getElementIcon(element: PsiElement): Icon? = null
}

