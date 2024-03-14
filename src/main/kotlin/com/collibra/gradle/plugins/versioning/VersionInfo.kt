package com.collibra.gradle.plugins.versioning

import org.semver4j.Semver

data class VersionInfo(
    val scm: String,
    val branch: String,
    val branchType: String,
    val branchId: String,
    val commit: String,
    val display: String?,
    val full: String,
    val base: String,
    val build: String,
    val time: String?,
    val tag: String?,
    val lastTag: String?,
    val dirty: Boolean,
    val shallow: Boolean,
    val versionNumber: VersionNumber?,
    val semver: Semver?
) {
    companion object {
        private val EMPTY: VersionInfo =
            VersionInfo("n/a", "", "", "", "", null, "", "", "", null, null, null, false, false, null, null)

        fun empty(): VersionInfo {
            return EMPTY
        }
    }

    fun isEmpty(): Boolean {
        return this == EMPTY
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }
}
