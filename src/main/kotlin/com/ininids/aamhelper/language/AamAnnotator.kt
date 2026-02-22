package com.ininids.aamhelper.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/** All built-in primitive type names */
private val BUILTIN_PRIMITIVES = setOf("i32", "f64", "string", "bool", "color")

/**
 * Namespaced types supported by aam_rs.
 * In .aam schema fields the path uses `types::math::vector2` etc.
 * The Rust resolver strips the leading `types::` and then dispatches on `math`, `time`, `physics`.
 * We accept both with and without the leading `types::` prefix so users aren't confused.
 */
private val BUILTIN_NAMESPACED_SHORT = setOf(
    // math
    "math::vector2", "math::vector3", "math::vector4",
    "math::quaternion", "math::matrix3x3", "math::matrix4x4",
    // time
    "time::datetime", "time::duration", "time::year", "time::day", "time::hour", "time::minute",
    // physics (subset used in autocompletion - full list accepted via prefix check)
    "physics::meter", "physics::kilogram", "physics::second", "physics::ampere",
    "physics::kelvin", "physics::mole", "physics::candela",
    "physics::squareMeter", "physics::cubicMeter",
    "physics::meterPerSecond", "physics::meterPerSecondSquared",
    "physics::radianPerSecond", "physics::radianPerSecondSquared",
    "physics::hertz", "physics::kilogramPerCubicMeter", "physics::kilogramMeterPerSecond",
    "physics::newton", "physics::newtonMeter", "physics::pascal", "physics::joule", "physics::watt",
    "physics::newtonPerMeter", "physics::dimensionless", "physics::kilogramSquareMeter",
    "physics::joulePerKilogramKelvin", "physics::joulePerKilogram", "physics::joulePerKelvin",
    "physics::coulomb", "physics::volt", "physics::ohm", "physics::ohmMeter", "physics::farad",
    "physics::voltPerMeter", "physics::tesla", "physics::weber", "physics::henry", "physics::siemens",
    "physics::coulombPerCubicMeter", "physics::coulombPerSquareMeter",
    "physics::faradPerMeter", "physics::henryPerMeter",
    "physics::amperePerMeter", "physics::amperePerSquareMeter",
    "physics::voltPerKelvin", "physics::pascalSecond", "physics::squareMeterPerSecond",
    "physics::newtonSecond", "physics::newtonPerCubicMeter", "physics::jouleSecond",
    "physics::kilogramPerMole", "physics::cubicMeterPerKilogram", "physics::meterPerCubicSecond",
    "physics::lumen", "physics::lux", "physics::lumenSecond", "physics::candelaPerSquareMeter",
    "physics::wattPerSteradian", "physics::wattPerSquareMeter", "physics::wattPerMeterKelvin",
    "physics::joulePerSquareMeter", "physics::radian", "physics::steradian",
    "physics::bit", "physics::decibel", "physics::katal", "physics::molePerCubicMeter",
    "physics::newtonPerMeterSquared", "physics::joulePerMole", "physics::joulePerMoleKelvin",
    "physics::kelvinPerWatt", "physics::kilogramPerSecond", "physics::cubicMeterPerSecond",
    "physics::inverseMeter", "physics::newtonPerCoulomb", "physics::weberPerMeter",
    "physics::teslaSquareMeter", "physics::arcDegree", "physics::arcMinute", "physics::arcSecond",
    "physics::bar", "physics::millimeterOfMercury", "physics::atmosphere", "physics::torr",
    "physics::poise", "physics::stokes", "physics::sverdrup", "physics::rayl", "physics::gal",
    "physics::maxwell", "physics::gauss", "physics::oersted", "physics::gilbert",
    "physics::franklin", "physics::debye", "physics::angstrom", "physics::lambert",
    "physics::phot", "physics::stilb", "physics::kayser", "physics::calorie",
    "physics::britishThermalUnit", "physics::langley", "physics::fahrenheit",
    "physics::celsius", "physics::rankine", "physics::curie", "physics::roentgen",
    "physics::rutherford", "physics::fermi", "physics::dalton",
    "physics::byte", "physics::baud", "physics::erlang",
    "physics::metabolicEquivalent", "physics::jansky", "physics::machNumber",
    "physics::knots", "physics::nauticalMile", "physics::horsepower",
    "physics::dioptre", "physics::percentage", "physics::becquerel", "physics::gray",
    "physics::sievert", "physics::electronVolt", "physics::barn",
    "physics::lightYear", "physics::parsec", "physics::astronomicalUnit", "physics::hubbleConstant"
)

private val KNOWN_NAMESPACES = setOf("math", "time", "physics")

/**
 * Returns true if [typeName] is a valid built-in type or a user-defined @type alias.
 * Accepts:
 *   - primitive: i32, f64, string, bool, color
 *   - short namespace: math::vector2, time::datetime, physics::second, …
 *   - long namespace: types::math::vector2, types::time::datetime, types::physics::second, …
 *   - @type aliases from the current file
 */
private fun isKnownType(typeName: String, aliases: Set<String>): Boolean {
    if (typeName in BUILTIN_PRIMITIVES) return true
    if (typeName in aliases) return true

    // Strip optional leading "types::" prefix
    val stripped = if (typeName.startsWith("types::")) typeName.removePrefix("types::") else typeName

    if (stripped in BUILTIN_NAMESPACED_SHORT) return true

    // Accept any types::<namespace>::<name> or <namespace>::<name> with a known namespace
    val parts = stripped.split("::")
    if (parts.size == 2 && parts[0] in KNOWN_NAMESPACES && parts[1].isNotBlank()) return true

    return false
}

private const val TYPE_HINT =
    "Accepted types: primitives (i32, f64, string, bool, color), " +
    "types::math::* (vector2…matrix4x4), " +
    "types::time::* (datetime, duration, year, day, hour, minute), " +
    "types::physics::* (meter, kilogram, second, …), or a @type alias."

class AamAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return

        // Collect all @type aliases defined in this file
        val aliases = PsiTreeUtil.getChildrenOfTypeAsList(file, AamTypeDeclaration::class.java)
            .mapNotNull { it.getAliasName() }.toSet()

        // ── Schema completeness: all required fields must have key-value pairs ──
        if (element is AamSchemaDeclaration) {
            val allProperties = PsiTreeUtil.getChildrenOfTypeAsList(file, AamProperty::class.java)
            val definedKeys = allProperties.mapNotNull { it.getKey() }.toSet()

            val schemaName = element.getSchemaName() ?: "unknown"
            for (field in element.getFields()) {
                val fieldName = field.getFieldName() ?: continue
                if (fieldName !in definedKeys) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Schema '$schemaName': required field '$fieldName' is not defined in this file"
                    ).range(element).create()
                }
            }
        }

        // ── Schema field type validation ──
        if (element is AamSchemaField) {
            val typeName = element.getFieldType() ?: return
            if (!isKnownType(typeName, aliases)) {
                val typeNode = element.node.findChildByType(AamTokenTypes.FIELD_TYPE)
                val range = typeNode?.psi ?: element
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Unknown type '$typeName'. $TYPE_HINT"
                ).range(range).create()
            }
        }

        // ── @type base type validation ──
        if (element is AamTypeDeclaration) {
            val baseType = element.getBaseType() ?: return
            if (!isKnownType(baseType, aliases)) {
                val baseNode = element.node.findChildByType(AamTokenTypes.TYPE_BASE)
                val range = baseNode?.psi ?: element
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Unknown base type '$baseType'. $TYPE_HINT"
                ).range(range).create()
            }
        }

        // ── @import / @derive file path validation ──
        if (element is AamImportStatement || element is AamDeriveStatement) {
            val pathText = when (element) {
                is AamImportStatement -> element.getFilePath()
                is AamDeriveStatement -> element.getFilePath()
                else -> null
            } ?: return

            val directive = if (element is AamImportStatement) "@import" else "@derive"

            if (pathText.isBlank()) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "$directive: file path cannot be empty"
                ).range(element).create()
                return
            }

            if (!pathText.endsWith(".aam")) {
                val pathNode = element.node.findChildByType(AamTokenTypes.FILE_PATH)
                val range = pathNode?.psi ?: element
                holder.newAnnotation(
                    HighlightSeverity.WARNING,
                    "$directive: expected a '.aam' file, got '$pathText'"
                ).range(range).create()
                return
            }

            // Check that the referenced file actually exists in the project
            val currentDir = element.containingFile.virtualFile?.parent
            if (currentDir != null) {
                val resolved = currentDir.findFileByRelativePath(pathText)
                if (resolved == null || !resolved.exists()) {
                    val pathNode = element.node.findChildByType(AamTokenTypes.FILE_PATH)
                    val range = pathNode?.psi ?: element
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "$directive: file '$pathText' not found"
                    ).range(range).create()
                }
            }
        }
    }
}
