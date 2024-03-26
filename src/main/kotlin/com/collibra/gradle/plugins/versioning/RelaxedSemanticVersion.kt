package com.collibra.gradle.plugins.versioning

import java.math.BigInteger

class RelaxedSemanticVersion(version: String?) {
    companion object {
        private const val STRICT_NUMERIC_IDENT = "0|[1-9]\\d*"
        private const val RELAXED_NUMERIC_IDENT = "\\d*"
        private const val NON_NUMERIC_IDENT = "\\d*[a-zA-Z-][a-zA-Z0-9-]*"
        private const val PRERELEASE_IDENT = "(?:$STRICT_NUMERIC_IDENT|$NON_NUMERIC_IDENT)"
        private const val BUILD_IDENT = "[0-9A-Za-z-]+"
        private const val STRICT_VERSION = "($STRICT_NUMERIC_IDENT)\\.($STRICT_NUMERIC_IDENT)\\.($STRICT_NUMERIC_IDENT)"
        private const val RELAXED_VERSION =
            "($STRICT_NUMERIC_IDENT)\\.?($RELAXED_NUMERIC_IDENT)?\\.?($STRICT_NUMERIC_IDENT)?"
        private const val PRERELEASE = "(?:-($PRERELEASE_IDENT(?:\\.$PRERELEASE_IDENT)*))"
        private const val BUILD = "(?:\\+($BUILD_IDENT(?:\\.$BUILD_IDENT)*))"
        private val YEAR_MONTH_REGEX = "^20\\d{2}\\.(0[1-9]|1[0-2])(?:\\.\\d*)?(?:\$|(-.*)|(\\+.*))".toRegex()
        private val STRICT_SEMVER_REGEX = "^v?$STRICT_VERSION$PRERELEASE?$BUILD?$".toRegex()
        private val RELAXED_SEMVER_REGEX = "^v?$RELAXED_VERSION$PRERELEASE?$BUILD?$".toRegex()
        private val COERCE_REGEX = "(^|\\D)(\\d{1,16})(?:\\.(\\d{1,16}))?(?:\\.(\\d{1,16}))?(?:$|\\D)".toRegex()
        private val LIST_SPLIT_REGEX = "\\.".toRegex()

        private fun parseInt(maybeInt: String?): Int? =
            if (!maybeInt.isNullOrBlank()) BigInteger(maybeInt).intValueExact() else null
    }

    private val major: Int?
    private val minor: Int?
    private val patch: Int?
    private val preRelease: List<String>
    private val build: List<String>
    private val qualifier: String
    private val equivalents: List<String>
    private val formatRelaxed: String
    private val formatStrict: String = "%d.%d.%d"
    private var isYearMonth: Boolean = false

    init {
        if (version.isNullOrBlank()) {
            major = null
            minor = null
            patch = null
            preRelease = emptyList()
            build = emptyList()
            qualifier = ""
            equivalents = emptyList()
            formatRelaxed = ""
        } else {
            val originalVersion = version.trim()
            val semverResult = RELAXED_SEMVER_REGEX.find(originalVersion)
            if (semverResult != null) {
                semverResult.destructured.let { (majorStr, minorStr, patchStr, preReleaseStr, buildStr) ->
                    major = parseInt(majorStr)
                    minor = parseInt(minorStr)
                    patch = parseInt(patchStr)
                    preRelease = preReleaseStr.takeIf { it.isNotBlank() }?.split(LIST_SPLIT_REGEX) ?: emptyList()
                    build = buildStr.takeIf { it.isNotBlank() }?.split(LIST_SPLIT_REGEX) ?: emptyList()
                }
            } else {
                val coerceResult = COERCE_REGEX.find(originalVersion)
                if (coerceResult != null) {
                    coerceResult.destructured.also { (_, majorStr, minorStr, patchStr, _) ->
                        major = parseInt(majorStr)
                        minor = parseInt(minorStr)
                        patch = parseInt(patchStr)
                    }
                } else {
                    major = null
                    minor = null
                    patch = null
                }
                preRelease = emptyList()
                build = emptyList()
            }
            var qualifierStr = ""
            if (preRelease.isNotEmpty()) {
                qualifierStr += "-${preRelease.joinToString(".")}"
            }
            if (build.isNotEmpty()) {
                qualifierStr += "+${build.joinToString(".")}"
            }
            qualifier = qualifierStr.trim()
            equivalents =
                if (major != null && preRelease.isEmpty() && build.isEmpty()) {
                    if (minor == null) {
                        listOf("$major.0.0", "$major.0", "$major")
                    } else if (patch == null) {
                        listOf("$major.$minor.0", "$major.$minor")
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            formatRelaxed =
                major?.let {
                    isYearMonth = YEAR_MONTH_REGEX.matches(originalVersion)
                    var versionFormat = if (isYearMonth) "%4d" else "%d"
                    if (minor != null) {
                        versionFormat += ".%"
                        if (isYearMonth) {
                            versionFormat += "02"
                        }
                        versionFormat += "d"
                        if (patch != null) {
                            versionFormat += ".%d"
                        }
                    }
                    versionFormat
                } ?: ""
        }
    }

    fun isEmpty(): Boolean {
        return major == null && minor == null && patch == null && preRelease.isEmpty() && build.isEmpty()
    }

    fun hasMajor(): Boolean = major != null

    fun hasMinor(): Boolean = minor != null

    fun hasPatch(): Boolean = patch != null

    fun isStrictNoQualifier(): Boolean = major != null && minor != null && patch != null && qualifier.isBlank()

    fun isYearMonth(): Boolean = isYearMonth

    fun major(): Int = major ?: 0

    fun minor(): Int = minor ?: 0

    fun patch(): Int = patch ?: 0

    fun preRelease(): List<String> = preRelease

    fun build(): List<String> = build

    fun qualifier(): String = qualifier

    fun equivalents(): List<String> = equivalents

    private fun relaxedVersion(explicitQualifier: String): String {
        return formatRelaxed.format(major, minor, patch) + explicitQualifier
    }

    fun relaxedVersion(): String {
        return relaxedVersion(qualifier)
    }

    fun strictVersion(): String {
        return formatStrict.format(major(), minor(), patch()) + qualifier
    }

    fun withQualifier(preRelease: String): RelaxedSemanticVersion {
        return RelaxedSemanticVersion(relaxedVersion("-$preRelease"))
    }

    fun withClearedQualifier(): RelaxedSemanticVersion {
        return RelaxedSemanticVersion(relaxedVersion(""))
    }

    override fun toString() = strictVersion()
}
