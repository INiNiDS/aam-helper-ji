package com.ininids.aamhelper.language

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class AamStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        if (element is NavigatablePsiElement) element.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean =
        element is NavigatablePsiElement && element.canNavigate()

    override fun canNavigateToSource(): Boolean =
        element is NavigatablePsiElement && element.canNavigateToSource()

    override fun getAlphaSortKey(): String = element.toString()

    override fun getPresentation(): ItemPresentation {
        return when (element) {
            is AamProperty -> PresentationData(element.getKey() ?: "", element.getValue(), null, null)
            is AamImportStatement -> PresentationData("@import ${element.getFilePath() ?: ""}", null, null, null)
            is AamDeriveStatement -> PresentationData("@derive ${element.getFilePath() ?: ""}", null, null, null)
            is AamSchemaDeclaration -> PresentationData("@schema ${element.getSchemaName() ?: ""}", null, null, null)
            is AamSchemaField -> PresentationData("${element.getFieldName() ?: ""}: ${element.getFieldType() ?: ""}", null, null, null)
            is AamFile -> PresentationData(element.name, null, null, null)
            else -> PresentationData(element.toString(), null, null, null)
        }
    }

    override fun getChildren(): Array<StructureViewTreeElement> {
        return when (element) {
            is AamFile -> {
                val children = ArrayList<StructureViewTreeElement>()
                PsiTreeUtil.getChildrenOfTypeAsList(element, AamSchemaDeclaration::class.java)
                    .forEach { children.add(AamStructureViewElement(it)) }
                PsiTreeUtil.getChildrenOfTypeAsList(element, AamImportStatement::class.java)
                    .forEach { children.add(AamStructureViewElement(it)) }
                PsiTreeUtil.getChildrenOfTypeAsList(element, AamDeriveStatement::class.java)
                    .forEach { children.add(AamStructureViewElement(it)) }
                PsiTreeUtil.getChildrenOfTypeAsList(element, AamProperty::class.java)
                    .forEach { children.add(AamStructureViewElement(it)) }
                children.toTypedArray()
            }
            is AamSchemaDeclaration -> {
                element.getFields().map { AamStructureViewElement(it) }.toTypedArray()
            }
            else -> emptyArray()
        }
    }
}