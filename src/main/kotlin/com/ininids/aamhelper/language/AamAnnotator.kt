package com.ininids.aamhelper.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

/** All built-in primitive type names */
private val BUILTIN_PRIMITIVES = setOf("i32", "f64", "string", "bool", "color")

private val BUILTIN_NAMESPACED_SHORT = setOf(
    "math::vector2", "math::vector3", "math::vector4",
    "math::quaternion", "math::matrix3x3", "math::matrix4x4",
    "time::datetime", "time::duration", "time::year", "time::day", "time::hour", "time::minute",
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
 * Returns true if [typeName] is a valid built-in type, a user-defined @type alias, or a schema name.
 * Also accepts list<T> — the T portion is checked recursively.
 */
private fun isKnownType(typeName: String, aliases: Set<String>, schemaNames: Set<String>): Boolean {
    // list<T>
    if (typeName.startsWith("list<") && typeName.endsWith(">")) {
        val inner = typeName.removePrefix("list<").removeSuffix(">").trim()
        return isKnownType(inner, aliases, schemaNames)
    }
    if (typeName in BUILTIN_PRIMITIVES) return true
    if (typeName in aliases) return true
    if (typeName in schemaNames) return true

    val stripped = if (typeName.startsWith("types::")) typeName.removePrefix("types::") else typeName
    if (stripped in BUILTIN_NAMESPACED_SHORT) return true

    val parts = stripped.split("::")
    return parts.size == 2 && parts[0] in KNOWN_NAMESPACES && parts[1].isNotBlank()
}

/**
 * Collects the set of field names from [schema] that are satisfied given [topLevelProperties]
 * and the map of all schemas [allSchemas] (for resolving nested schema types).
 *
 * A field is satisfied if:
 * - There is a top-level property with matching key, OR
 * - There is a top-level property whose inline value contains a key matching this field name, OR
 * - A top-level property key matches this field name AND its inline value recursively satisfies
 *   the nested schema fields (those missing fields are reported separately via [onMissingNested]).
 *
 * [visited] prevents infinite recursion for circular schema references.
 */
private fun collectSatisfiedFields(
    schema: AamSchemaDeclaration,
    topLevelProperties: List<AamProperty>,
    allSchemas: Map<String, AamSchemaDeclaration>,
    visited: MutableSet<String> = mutableSetOf()
): Set<String> {
    val schemaName = schema.getSchemaName() ?: return emptySet()
    if (schemaName in visited) return emptySet()
    visited.add(schemaName)

    // Build a map of top-level key -> property
    val propByKey = topLevelProperties.associateBy { it.getKey() ?: "" }

    val satisfied = mutableSetOf<String>()

    for (field in schema.getFields()) {
        val fieldName = field.getFieldName() ?: continue

        // Case 1: direct top-level key
        if (fieldName in propByKey) {
            satisfied.add(fieldName)
            continue
        }

        // Case 2: the field is inside an inline object at the top level
        // e.g. schema field "name" can be satisfied by server = { name = ... }
        // where "server" key wraps an inline object whose keys include "name"
        val foundInInline = topLevelProperties.any { prop ->
            val inline = prop.getInlineValue() ?: return@any false
            containsKeyRecursively(fieldName, inline)
        }
        if (foundInInline) {
            satisfied.add(fieldName)
            continue
        }

        // Case 3: if the field type is a schema name, check whether a matching inline object
        // provides all required keys for that nested schema
        val fieldType = field.getFieldType()
        if (fieldType != null && fieldType in allSchemas) {
            val nestedSchema = allSchemas[fieldType]!!
            // look for a top-level property that could wrap this schema inline
            for (prop in topLevelProperties) {
                val inline = prop.getInlineValue() ?: continue
                val nestedSatisfied = collectSatisfiedFieldsFromInline(nestedSchema, inline, allSchemas, visited.toMutableSet())
                val nestedRequired = nestedSchema.getFields().filter { !it.isOptional() }.mapNotNull { it.getFieldName() }.toSet()
                if (nestedRequired.isNotEmpty() && nestedSatisfied.containsAll(nestedRequired)) {
                    satisfied.add(fieldName)
                    break
                }
            }
        }
    }

    return satisfied
}

/**
 * Checks whether [fieldName] appears as a direct or nested key anywhere inside [inline].
 */
private fun containsKeyRecursively(fieldName: String, inline: AamInlineValue): Boolean {
    for ((key, nested) in inline.getProperties()) {
        if (key == fieldName) return true
        if (nested != null && containsKeyRecursively(fieldName, nested)) return true
    }
    return false
}

/**
 * Collects satisfied field names of [schema] by examining keys inside [inline].
 */
private fun collectSatisfiedFieldsFromInline(
    schema: AamSchemaDeclaration,
    inline: AamInlineValue,
    allSchemas: Map<String, AamSchemaDeclaration>,
    visited: MutableSet<String>
): Set<String> {
    val schemaName = schema.getSchemaName() ?: return emptySet()
    if (schemaName in visited) return emptySet()
    visited.add(schemaName)

    val propsByKey = inline.getProperties().associateBy { it.first }
    val satisfied = mutableSetOf<String>()

    for (field in schema.getFields()) {
        val fieldName = field.getFieldName() ?: continue

        if (fieldName in propsByKey) {
            satisfied.add(fieldName)
            continue
        }

        // recurse into nested inline objects within this inline
        val foundInNested = propsByKey.values.any { (_, nestedInline) ->
            nestedInline != null && containsKeyRecursively(fieldName, nestedInline)
        }
        if (foundInNested) {
            satisfied.add(fieldName)
            continue
        }

        // nested schema type inside inline
        val fieldType = field.getFieldType()
        if (fieldType != null && fieldType in allSchemas) {
            val nestedSchema = allSchemas[fieldType]!!
            for ((_, nestedInline) in propsByKey.values) {
                if (nestedInline == null) continue
                val nestedSatisfied = collectSatisfiedFieldsFromInline(nestedSchema, nestedInline, allSchemas, visited.toMutableSet())
                val nestedRequired = nestedSchema.getFields().filter { !it.isOptional() }.mapNotNull { it.getFieldName() }.toSet()
                if (nestedRequired.isNotEmpty() && nestedSatisfied.containsAll(nestedRequired)) {
                    satisfied.add(fieldName)
                    break
                }
            }
        }
    }

    return satisfied
}

private const val TYPE_HINT =
    "Accepted types: primitives (i32, f64, string, bool, color), " +
    "types::math::* (vector2…matrix4x4), " +
    "types::time::* (datetime, duration, year, day, hour, minute), " +
    "types::physics::* (meter, kilogram, second, …), a @type alias, " +
    "another @schema name, or list<T>."

class AamAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return

        // Collect all @type aliases defined in this file
        val aliases = PsiTreeUtil.getChildrenOfTypeAsList(file, AamTypeDeclaration::class.java)
            .mapNotNull { it.getAliasName() }.toSet()

        // Collect all schema names defined in this file
        val schemaNames = PsiTreeUtil.getChildrenOfTypeAsList(file, AamSchemaDeclaration::class.java)
            .mapNotNull { it.getSchemaName() }.toSet()

        // ── Schema completeness: required fields must be defined in the file that calls @derive ────
        if (element is AamDeriveStatement) {
            val pathText = element.getFilePath() ?: return
            if (!pathText.endsWith(".aam")) return

            val currentDir = element.containingFile.virtualFile?.parent ?: return
            val targetVFile = currentDir.findFileByRelativePath(pathText) ?: return
            if (!targetVFile.exists()) return

            val targetPsiFile = PsiManager.getInstance(element.project).findFile(targetVFile) ?: return
            val allSchemas = PsiTreeUtil.getChildrenOfTypeAsList(targetPsiFile, AamSchemaDeclaration::class.java)

            // Build a map of all schema names -> declaration for nested type resolution
            val allSchemasByName = allSchemas.associateBy { it.getSchemaName() ?: "" }

            val selectiveSchema = element.getSelectiveSchema()
            val schemasToCheck = if (selectiveSchema != null)
                allSchemas.filter { it.getSchemaName() == selectiveSchema }
            else
                allSchemas

            val topLevelProperties = PsiTreeUtil.getChildrenOfTypeAsList(file, AamProperty::class.java)

            for (schema in schemasToCheck) {
                val schemaName = schema.getSchemaName() ?: "unknown"
                val satisfiedFields = collectSatisfiedFields(schema, topLevelProperties, allSchemasByName)
                for (field in schema.getFields()) {
                    if (field.isOptional()) continue
                    val fieldName = field.getFieldName() ?: continue
                    if (fieldName !in satisfiedFields) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            "Schema '$schemaName': required field '$fieldName' is not defined in this file"
                        ).range(element).create()
                    }
                }
            }
        }

        // ── Schema field type validation ──
        if (element is AamSchemaField) {
            val fieldType = element.getFieldType() ?: return
            val isListField = element.isListType()
            val displayType = if (isListField) "list<$fieldType>" else fieldType
            if (!isKnownType(displayType, aliases, schemaNames)) {
                val typeNode = element.node.findChildByType(AamTokenTypes.FIELD_TYPE)
                val range = typeNode?.psi ?: element
                holder.newAnnotation(HighlightSeverity.ERROR, "Unknown type '$displayType'. $TYPE_HINT")
                    .range(range).create()
            }
        }

        // ── @type base type validation ──
        if (element is AamTypeDeclaration) {
            val baseType = element.getBaseType() ?: return
            if (!isKnownType(baseType, aliases, schemaNames)) {
                val baseNode = element.node.findChildByType(AamTokenTypes.TYPE_BASE)
                val range = baseNode?.psi ?: element
                holder.newAnnotation(HighlightSeverity.ERROR, "Unknown base type '$baseType'. $TYPE_HINT")
                    .range(range).create()
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
                holder.newAnnotation(HighlightSeverity.ERROR, "$directive: file path cannot be empty")
                    .range(element).create()
                return
            }

            if (!pathText.endsWith(".aam")) {
                val pathNode = element.node.findChildByType(AamTokenTypes.FILE_PATH)
                val range = pathNode?.psi ?: element
                holder.newAnnotation(HighlightSeverity.WARNING, "$directive: expected a '.aam' file, got '$pathText'")
                    .range(range).create()
                return
            }

            // Check that the referenced file actually exists in the project
            val currentDir = element.containingFile.virtualFile?.parent
            if (currentDir != null) {
                val resolved = currentDir.findFileByRelativePath(pathText)
                if (resolved == null || !resolved.exists()) {
                    val pathNode = element.node.findChildByType(AamTokenTypes.FILE_PATH)
                    val range = pathNode?.psi ?: element
                    holder.newAnnotation(HighlightSeverity.ERROR, "$directive: file '$pathText' not found")
                        .range(range).create()
                }
            }

            // ── Validate selective schema exists in the target file ──
            if (element is AamDeriveStatement) {
                val selectiveSchema = element.getSelectiveSchema()
                if (selectiveSchema != null && currentDir != null) {
                    val targetVFile = currentDir.findFileByRelativePath(pathText)
                    if (targetVFile != null && targetVFile.exists()) {
                        val targetPsiFile = PsiManager.getInstance(element.project).findFile(targetVFile)
                        if (targetPsiFile != null) {
                            val targetSchemas = PsiTreeUtil.getChildrenOfTypeAsList(targetPsiFile, AamSchemaDeclaration::class.java)
                                .mapNotNull { it.getSchemaName() }.toSet()
                            if (selectiveSchema !in targetSchemas) {
                                val schemaNode = element.node.findChildByType(AamTokenTypes.DERIVE_SCHEMA)
                                val range = schemaNode?.psi ?: element
                                holder.newAnnotation(HighlightSeverity.ERROR,
                                    "@derive: schema '$selectiveSchema' not found in '$pathText'")
                                    .range(range).create()
                            }
                        }
                    }
                }
            }
        }
    }
}
