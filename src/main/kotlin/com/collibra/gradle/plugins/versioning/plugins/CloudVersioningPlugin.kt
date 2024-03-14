package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.ReleaseInfo
import com.collibra.gradle.plugins.versioning.SCMInfo
import com.collibra.gradle.plugins.versioning.VersionInfo
import com.collibra.gradle.plugins.versioning.VersioningExtension
import com.collibra.gradle.plugins.versioning.VersioningPlugin
import org.gradle.api.Project

class CloudVersioningPlugin : VersioningPlugin() {
    companion object {
        private const val DEFAULT_AV_BRANCHES = "$MAIN_BRANCH_TYPE,$PRE_BRANCH_TYPE,$RELEASE_BRANCH_TYPE"
    }

    override fun configureExtension(project: Project, extension: VersioningExtension) {
        val avBranches = autoVersionBranches(project, DEFAULT_AV_BRANCHES)
        extension.releaseParser = { scmInfo: SCMInfo, separator: String? ->
            val releaseInfo = ReleaseInfo.fromBranch(scmInfo.branch!!, separator)
            if (avBranches.contains(scmInfo.branch) || avBranches.contains(releaseInfo.branchType)) {
                ReleaseInfo(
                    RELEASE_BRANCH_TYPE,
                    if (releaseInfo.branchType == RELEASE_BRANCH_TYPE || releaseInfo.branchType == PRE_BRANCH_TYPE)
                        releaseInfo.versionBase
                    else baseVersion(project)
                )
            } else {
                ReleaseInfo("disabled", scmInfo.branch)
            }
        }
    }

    override fun buildVersion(info: VersionInfo): String {
        // return the semantic version
        return info.semver.toString()
    }
}
