package com.ininids.aamhelper.language

import com.intellij.lang.Language

object AamLanguage : Language("AAM") {
    private fun readResolve(): Any = AamLanguage
}