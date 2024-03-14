package com.collibra.gradle.plugins.versioning

data class ReleaseInfo(val branchType: String, val versionBase: String) {
    companion object {
        fun fromBranch(branch: String, separator: String?): ReleaseInfo {
            val parts = branch.split((separator ?: "/").toRegex(), limit = 2)
            return ReleaseInfo(if (parts.isNotEmpty()) parts[0] else "", if (parts.size > 1) parts[1] else "")
        }
    }
}
