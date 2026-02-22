package com.ininids.aamhelper.language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AamFileType : LanguageFileType(AamLanguage) {
    override fun getName(): String = "AAM File"
    override fun getDescription(): String = "AAM language file"
    override fun getDefaultExtension(): String = "aam"
    override fun getIcon(): Icon = AamIcons.FILE
}
