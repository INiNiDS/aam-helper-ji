package com.ininids.aamhelper.language

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class AamFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AamLanguage) {
    override fun getFileType(): FileType = AamFileType
    override fun toString(): String = "AAM File"
}
