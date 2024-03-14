package com.collibra.gradle.plugins.versioning

data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val qualifier: String,
    val versionCode: Int,
    val versionString: String
)
