package com.ininids.aamhelper.language

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.intellij.patterns.PlatformPatterns

class AamReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // FILE_PATH tokens for both @import and @derive
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(AamTokenTypes.FILE_PATH),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val value = element.text
                    return arrayOf(AamFileReference(element, TextRange(0, value.length)))
                }
            }
        )
    }
}

class AamFileReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val fileName = element.text
        val project = element.project

        val virtualFiles = com.intellij.psi.search.FilenameIndex.getVirtualFilesByName(
            fileName,
            com.intellij.psi.search.GlobalSearchScope.allScope(project)
        )

        val virtualFile = virtualFiles.firstOrNull() ?: return null
        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }
}
