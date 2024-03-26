package com.collibra.gradle.plugins.versioning

data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val qualifier: String,
    val versionString: String
) {
    val versionCode = major * 10000 + minor * 100 + patch
}
