package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.ReleaseInfo
import com.collibra.gradle.plugins.versioning.SCMInfo
import com.collibra.gradle.plugins.versioning.VersionInfo
import com.collibra.gradle.plugins.versioning.VersioningExtension
import com.collibra.gradle.plugins.versioning.VersioningPlugin
import org.gradle.api.Project

class EdgeVersioningPlugin : VersioningPlugin() {
    companion object {
        private const val DEFAULT_AV_BRANCHES = "$MAIN_BRANCH_TYPE,$RELEASE_BRANCH_TYPE"
    }

    override fun configureExtension(project: Project, extension: VersioningExtension) {
        val baseVersion = baseVersion(project)
        val avBranches = autoVersionBranches(project, DEFAULT_AV_BRANCHES)
        // configure the release parser
        extension.releaseParser = { scmInfo: SCMInfo, separator: String? ->
            val releaseInfo = ReleaseInfo.fromBranch(scmInfo.branch!!, separator)
            if (releaseInfo.branchType == RELEASE_BRANCH_TYPE) {
                // release branch always gets its version from the branchId, by convention: x.y
                ReleaseInfo(RELEASE_BRANCH_TYPE, releaseInfo.versionBase)
            } else if (scmInfo.branch == MAIN_BRANCH_TYPE) {
                // baseVersion can be 1 or 2 digit
                ReleaseInfo(releaseInfo.branchType, baseVersion)
            } else {
                ReleaseInfo("disabled", scmInfo.branch)
            }
        }
        // configure to have auto-versioning behaviour for both main and release
        extension.releases = avBranches
        // if single number baseVersion, change the lastTagPattern. This is used to look for
        // matching tags
        if (isSingleNumberVersion(baseVersion)) {
            // needed to find matching tags for autoIncrement minor, so that later we can increment
            // the minor ourselves
            extension.lastTagPattern = "$baseVersion\\.(\\d+)\\.\\d+$"
        }
    }

    override fun buildVersion(info: VersionInfo): String {
        // release branch or main branch with patch-increment behaviour
        val isAutoIncrementMinor = isSingleNumberVersion(info.base)
        return if (
            info.branchType == RELEASE_BRANCH_TYPE || (!isAutoIncrementMinor) && info.branchType == MAIN_BRANCH_TYPE
        ) {
            info.display ?: "0.0.1"
            // auto-increment patch (done by base classes and put in .display) or initial
            // (default scenario for patch)
            // main branch with minor-increment behaviour
        } else if (isAutoIncrementMinor && info.branchType == MAIN_BRANCH_TYPE) {
            // minor-increment the last matching tag
            if (!info.lastTag.isNullOrEmpty()) {
                // logic to increment the minor part of a semantic version
                // assuming releasable semantic versioning tags
                val parts = info.lastTag.split('.')
                val major = parts[0]
                val minor = parts[1]
                major + "." + (minor.toInt() + 1) + ".0"
            } else { // no matching tag existing yet
                val baseVersion = info.base
                // new major required if baseVersion is not 0
                if (baseVersion != "0") { // major
                    "$baseVersion.0.0"
                    // initial scenario for minor if baseVersion is 0
                } else { // initial
                    "0.1.0"
                }
            }
            // no auto-versioning applied
        } else {
            (info.lastTag ?: "0.0.1") + "-" + info.build
        }
    }
}
