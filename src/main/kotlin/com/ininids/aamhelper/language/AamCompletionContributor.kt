package com.ininids.aamhelper.language

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.search.FilenameIndex
import com.intellij.util.ProcessingContext

private val PRIMITIVE_TYPES = listOf("i32", "f64", "string", "bool", "color")

private val MATH_TYPES = listOf(
    "types::math::vector2", "types::math::vector3", "types::math::vector4",
    "types::math::quaternion", "types::math::matrix3x3", "types::math::matrix4x4"
)

private val TIME_TYPES = listOf(
    "types::time::datetime", "types::time::duration", "types::time::year",
    "types::time::day", "types::time::hour", "types::time::minute"
)

private val PHYSICS_TYPES = listOf(
    "types::physics::meter", "types::physics::kilogram", "types::physics::second",
    "types::physics::ampere", "types::physics::kelvin", "types::physics::mole", "types::physics::candela",
    "types::physics::squareMeter", "types::physics::cubicMeter",
    "types::physics::meterPerSecond", "types::physics::meterPerSecondSquared",
    "types::physics::radianPerSecond", "types::physics::radianPerSecondSquared",
    "types::physics::hertz", "types::physics::kilogramPerCubicMeter", "types::physics::kilogramMeterPerSecond",
    "types::physics::newton", "types::physics::newtonMeter", "types::physics::pascal",
    "types::physics::joule", "types::physics::watt",
    "types::physics::newtonPerMeter", "types::physics::dimensionless", "types::physics::kilogramSquareMeter",
    "types::physics::joulePerKilogramKelvin", "types::physics::joulePerKilogram", "types::physics::joulePerKelvin",
    "types::physics::coulomb", "types::physics::volt", "types::physics::ohm", "types::physics::ohmMeter",
    "types::physics::farad", "types::physics::voltPerMeter", "types::physics::tesla",
    "types::physics::weber", "types::physics::henry", "types::physics::siemens",
    "types::physics::coulombPerCubicMeter", "types::physics::coulombPerSquareMeter",
    "types::physics::faradPerMeter", "types::physics::henryPerMeter",
    "types::physics::amperePerMeter", "types::physics::amperePerSquareMeter",
    "types::physics::voltPerKelvin", "types::physics::pascalSecond", "types::physics::squareMeterPerSecond",
    "types::physics::newtonSecond", "types::physics::newtonPerCubicMeter", "types::physics::jouleSecond",
    "types::physics::kilogramPerMole", "types::physics::cubicMeterPerKilogram", "types::physics::meterPerCubicSecond",
    "types::physics::lumen", "types::physics::lux", "types::physics::lumenSecond",
    "types::physics::candelaPerSquareMeter", "types::physics::wattPerSteradian",
    "types::physics::wattPerSquareMeter", "types::physics::wattPerMeterKelvin",
    "types::physics::joulePerSquareMeter", "types::physics::radian", "types::physics::steradian",
    "types::physics::bit", "types::physics::decibel", "types::physics::katal",
    "types::physics::molePerCubicMeter", "types::physics::newtonPerMeterSquared",
    "types::physics::joulePerMole", "types::physics::joulePerMoleKelvin",
    "types::physics::kelvinPerWatt", "types::physics::kilogramPerSecond", "types::physics::cubicMeterPerSecond",
    "types::physics::inverseMeter", "types::physics::newtonPerCoulomb", "types::physics::weberPerMeter",
    "types::physics::teslaSquareMeter", "types::physics::arcDegree", "types::physics::arcMinute",
    "types::physics::arcSecond", "types::physics::bar", "types::physics::millimeterOfMercury",
    "types::physics::atmosphere", "types::physics::torr", "types::physics::poise", "types::physics::stokes",
    "types::physics::sverdrup", "types::physics::rayl", "types::physics::gal",
    "types::physics::maxwell", "types::physics::gauss", "types::physics::oersted", "types::physics::gilbert",
    "types::physics::franklin", "types::physics::debye", "types::physics::angstrom", "types::physics::lambert",
    "types::physics::phot", "types::physics::stilb", "types::physics::kayser", "types::physics::calorie",
    "types::physics::britishThermalUnit", "types::physics::langley", "types::physics::fahrenheit",
    "types::physics::celsius", "types::physics::rankine", "types::physics::curie", "types::physics::roentgen",
    "types::physics::rutherford", "types::physics::fermi", "types::physics::dalton",
    "types::physics::byte", "types::physics::baud", "types::physics::erlang",
    "types::physics::metabolicEquivalent", "types::physics::jansky", "types::physics::machNumber",
    "types::physics::knots", "types::physics::nauticalMile", "types::physics::horsepower",
    "types::physics::dioptre", "types::physics::percentage", "types::physics::becquerel",
    "types::physics::gray", "types::physics::sievert", "types::physics::electronVolt", "types::physics::barn",
    "types::physics::lightYear", "types::physics::parsec", "types::physics::astronomicalUnit",
    "types::physics::hubbleConstant"
)

private val ALL_TYPES = PRIMITIVE_TYPES + MATH_TYPES + TIME_TYPES + PHYSICS_TYPES

class AamCompletionContributor : CompletionContributor() {
    init {
        // Complete directive keywords when typing a KEY token
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AamTokenTypes.KEY),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // Build a handler that replaces the entire KEY token text (e.g. "@") with the directive
                    val insertHandler = InsertHandler<com.intellij.codeInsight.lookup.LookupElement> { ctx, item ->
                        val doc = ctx.document
                        val startOffset = ctx.startOffset
                        val tailOffset = ctx.tailOffset
                        // Replace the full range that was originally the KEY token
                        val tokenStart = parameters.position.textRange.startOffset
                        val tokenEnd = parameters.position.textRange.endOffset - 1 // -1 for dummy suffix
                        doc.replaceString(tokenStart, tailOffset, item.lookupString)
                        ctx.editor.caretModel.moveToOffset(tokenStart + item.lookupString.length)
                    }

                    val directives = listOf("@import", "@derive", "@schema", "@type")
                    // The position text contains the dummy completion identifier appended by IDE
                    val rawText = parameters.position.text
                    val typed = rawText.removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)

                    val prefixResult = result.withPrefixMatcher(typed)
                    directives.forEach { directive ->
                        prefixResult.addElement(
                            LookupElementBuilder.create(directive)
                                .withPresentableText(directive)
                                .withInsertHandler(insertHandler)
                        )
                    }
                }
            }
        )

        // Complete .aam file paths for @import / @derive FILE_PATH tokens
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AamTokenTypes.FILE_PATH),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val allFilenames = FilenameIndex.getAllFilenames(project)
                    for (filename in allFilenames) {
                        if (filename.endsWith(".aam")) {
                            result.addElement(LookupElementBuilder.create(filename))
                        }
                    }
                }
            }
        )

        // Complete known schema field types (including math::, time::, physics::)
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AamTokenTypes.FIELD_TYPE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val file = parameters.originalFile
                    // Also suggest @type aliases defined in this file
                    val aliases = com.intellij.psi.util.PsiTreeUtil
                        .getChildrenOfTypeAsList(file, AamTypeDeclaration::class.java)
                        .mapNotNull { it.getAliasName() }

                    (ALL_TYPES + aliases).forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
        )

        // Complete base types for @type alias = <TYPE_BASE>
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AamTokenTypes.TYPE_BASE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    ALL_TYPES.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
        )
    }
}
