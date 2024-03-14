package com.collibra.gradle.plugins.versioning

import com.collibra.gradle.plugins.versioning.VersioningExtension.Companion.DisplayModes
import com.collibra.gradle.plugins.versioning.VersioningExtension.Companion.ReleaseModes
import com.collibra.gradle.plugins.versioning.git.GitInfoService
import com.collibra.gradle.plugins.versioning.support.DirtyException
import com.collibra.gradle.plugins.versioning.tasks.VersionDisplayTask
import com.collibra.gradle.plugins.versioning.tasks.VersionFileTask
import groovy.lang.Closure
import kotlin.jvm.optionals.getOrNull
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class VersioningPlugin : Plugin<Project> {
    companion object {
        private const val AUTO_VERSION_BRANCHES_PROP = "autoVersionBranches"
        private const val BASE_VERSION_PROP = "baseVersion"

        // Do not use a `.` as part of the prefix here, as that functions as a separator for
        // prerelease identifiers.
        const val PRE_RELEASE_PREFIX = "sha-"
        const val DEFAULT_VERSION = "0.0.1"
        const val MAIN_BRANCH_TYPE = "main"
        const val PRE_BRANCH_TYPE = "pre"
        const val RELEASE_BRANCH_TYPE = "release"

        private val SINGLE_NUMBER_VERSION_REGEX = "^\\d+$".toRegex()
        private val NORMALIZE_REGEX = "[^A-Za-z0-9.\\-_]".toRegex()

        fun baseVersion(project: Project): String {
            return readRequiredProperty(project, BASE_VERSION_PROP)
        }

        fun baseVersion(project: Project, defaultValue: Int): String {
            return readOptionalProperty(project, BASE_VERSION_PROP, defaultValue.toString())
        }

        fun autoVersionBranches(project: Project, defaultValue: String): Set<String> {
            return LinkedHashSet(
                readOptionalProperty(project, AUTO_VERSION_BRANCHES_PROP, defaultValue).split(",").map { it.trim() }
            )
        }

        fun isSingleNumberVersion(version: String): Boolean {
            return SINGLE_NUMBER_VERSION_REGEX.matches(version)
        }

        fun normalize(value: String?): String {
            return value!!.replace(NORMALIZE_REGEX, "-")
        }

        fun computeInfo(project: Project, extension: VersioningExtension): VersionInfo {
            return computeInfo(project, extension, GitInfoService.getInfo(project, extension), null)
        }

        fun computeInfo(
            project: Project,
            extension: VersioningExtension,
            scmInfo: SCMInfo,
            scmTags: List<String>?
        ): VersionInfo {
            if (scmInfo.isEmpty()) {
                return VersionInfo.empty()
            }
            with(extension) {
                val branchInfo = releaseParser(scmInfo, "/")
                val branchType = branchInfo.branchType
                val branchId = normalize(scmInfo.branch)
                val versionBase = branchInfo.versionBase
                var versionFull = full(scmInfo).toString()
                var versionDisplay =
                    if (releases.contains(branchType)) {
                        if (scmInfo.shallow) {
                            // In case the repository has no history (shallow clone or check out),
                            // the last tags cannot be gotten and the display version cannot be
                            // computed correctly. The only special case is when the HEAD commit is
                            // exactly on a tag and we can use it, in any other case, we can only
                            // start from the base information and add a snapshot information
                            scmInfo.takeIf { releaseBuild }?.tag ?: "$versionBase$snapshot"
                        } else {
                            val lastTagPatternRegex = "^$versionBase\\.(\\d+)$".toRegex()
                            val lastTag =
                                scmTags?.stream()?.let {
                                    GitInfoService.filterAndSortTags(lastTagPatternRegex, it).findFirst().getOrNull()
                                } ?: GitInfoService.getLastTag(project, this, lastTagPatternRegex) ?: ""
                            val nextTagIncrement =
                                lastTagPatternRegex.find(lastTag)?.let { it.groupValues[1].toInt() + 1 } ?: 0
                            val nextTag = "$versionBase.$nextTagIncrement"
                            when (val mode = releaseMode) {
                                is ReleaseModes,
                                is String -> {
                                    ReleaseModes.valueOf(mode.toString().uppercase())
                                        .toDisplayVersion(nextTag, lastTag, scmInfo.tag, this)
                                }
                                is Closure<*> -> {
                                    mode.call(nextTag, lastTag, scmInfo.tag, this).toString()
                                }
                                else -> {
                                    throw GradleException(
                                        "The `releaseMode` must be a registered default mode or a Closure."
                                    )
                                }
                            }
                        }
                    } else {
                        // Adjusting the base
                        val branchBase = versionBase.ifEmpty { branchId }
                        // Display mode
                        when (val mode = displayMode) {
                            is DisplayModes,
                            is String -> {
                                DisplayModes.valueOf(mode.toString().uppercase())
                                    .toDisplayVersion(
                                        branchType,
                                        branchId,
                                        branchBase,
                                        scmInfo.abbreviated,
                                        versionFull,
                                        this
                                    )
                            }
                            is Closure<*> -> {
                                mode
                                    .call(branchType, branchId, branchBase, scmInfo.abbreviated, versionFull, this)
                                    .toString()
                            }
                            else -> {
                                throw GradleException(
                                    "The `displayMode` must be a registered default mode or a Closure."
                                )
                            }
                        }
                    }

                // Dirty update
                if (scmInfo.dirty) {
                    if (dirtyStatusLog) {
                        val status = scmInfo.status
                        project.logger.warn("[versioning] WARNING - git status:")
                        mapOf(
                                "staged" to status!!.staged.allChanges,
                                "unstaged" to status.unstaged.allChanges,
                                "conflicts" to status.conflicts
                            )
                            .forEach { (type: String, changes: Set<String?>?) ->
                                if (changes != null && changes.isNotEmpty()) {
                                    project.logger.warn("$type [\n\t{}\n]", java.lang.String.join("\n\t", changes))
                                }
                            }
                    }

                    if (dirtyFailOnReleases && releases.contains(branchType)) {
                        throw DirtyException()
                    } else {
                        if (!noWarningOnDirty) {
                            project.logger.warn(
                                "[versioning] WARNING - the working copy has un-staged or un-committed changes."
                            )
                        }

                        versionDisplay = dirty(versionDisplay).toString()
                        versionFull = dirty(versionFull).toString()
                    }
                }

                // OK
                return VersionInfo(
                    "git",
                    scmInfo.branch!!,
                    branchType,
                    branchId,
                    scmInfo.commit!!,
                    versionDisplay,
                    versionFull,
                    versionBase,
                    scmInfo.abbreviated!!,
                    computeTimestamp(scmInfo.dateTime),
                    scmInfo.tag,
                    scmInfo.lastTag,
                    scmInfo.dirty,
                    scmInfo.shallow,
                    parseVersionNumber(scmInfo, branchType, branchId, versionFull, versionBase, versionDisplay),
                    computeSemantic(scmInfo, branchInfo, versionDisplay)
                )
            }
        }
    }

    open fun configureExtension(project: Project, extension: VersioningExtension) {
        // use the defaults
    }

    open fun buildVersion(info: VersionInfo): String? {
        // return the display version by default
        return info.display
    }

    override fun apply(project: Project) {
        // create the extension and tasks
        val extension = project.extensions.create("versioning", VersioningExtension::class.java, project)
        project.tasks.register("versionDisplay", VersionDisplayTask::class.java)
        project.tasks.register("versionFile", VersionFileTask::class.java)

        // allow the implementation class to configure the extension
        configureExtension(project, extension)

        // set version only if we have info from SCM
        extension.info.let { info ->
            if (info.isNotEmpty()) {
                buildVersion(info)?.let { v ->
                    project.allprojects.forEach { p ->
                        if (p.version == Project.DEFAULT_VERSION) {
                            p.version = v
                        }
                    }
                }
            }
        }
    }
}
