package com.collibra.gradle.plugins.versioning

import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrDefault
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class VersioningPlugin : Plugin<Project> {
    companion object {
        private const val BASE_VERSION_PROP = "baseVersion"
        private const val BUILD_NUMBER_ENV = "BUILD_NUMBER"

        // Do not use a `.` as part of the prefix here, as that functions as a separator for
        // prerelease identifiers in semantic versioning
        private const val PRE_RELEASE_PREFIX = "sha-"

        private const val MAIN_BRANCH_TYPE = "main"
        private const val MAIN_BRANCH_SUBTYPE_MASTER = "master"
        private const val RELEASE_BRANCH_TYPE = "release"
        private const val RELEASE_BRANCH_SUBTYPE_PRE = "pre"

        private val SPLIT_BRANCH_REGEX = "/".toRegex()
        private val NORMALIZE_BRANCH_REGEX = "[^A-Za-z0-9.\\-_]".toRegex()

        fun computeInfo(project: Project, extension: VersioningExtension): VersionInfo {
            // Check if git is available, if not return an empty version info
            if (!GitRepositoryInfo.checkGitDirectory(project, extension)) {
                return VersionInfo.empty()
            }

            // Open the git repository ensuring it is closed after the block
            GitRepositoryInfo.openGit(project, extension).use { git ->
                // Gather the branch and tag information from git
                val branchName = GitRepositoryInfo.getBranchName(git, extension)!!
                val tagRefs = GitRepositoryInfo.getTagRefs(git)
                val tagNames = GitRepositoryInfo.getTagNames(git, tagRefs, branchName)
                val gitInfo = GitRepositoryInfo.buildInfo(git, branchName, tagRefs, tagNames)
                val branchParts = gitInfo.branch.split(SPLIT_BRANCH_REGEX, limit = 2)
                val branchType = if (branchParts.isNotEmpty()) branchParts[0] else ""
                val isReleaseBranch = branchType == RELEASE_BRANCH_TYPE || branchType == RELEASE_BRANCH_SUBTYPE_PRE
                val isTrunkBranch = branchType == MAIN_BRANCH_TYPE || branchType == MAIN_BRANCH_SUBTYPE_MASTER
                val branchId = gitInfo.branch.replace(NORMALIZE_BRANCH_REGEX, "-")

                // Build the full version based on the branch identifier, the short commit
                // identifier, and the cleanliness state of the repository
                val versionDirtySuffix =
                    dirtyVersionSuffix(project.logger, gitInfo, isReleaseBranch && extension.dirtyFailOnReleases)
                val versionPreReleaseSuffix = "$PRE_RELEASE_PREFIX${gitInfo.abbreviated}$versionDirtySuffix"
                val versionFull = "$branchId-$versionPreReleaseSuffix"

                // Build the base version, which is used to locate tags and to compute
                // the next version. The base version is determined using the following rules:
                // 1. If Jenkins mode is enabled and the project version is semantic, the base
                //    version is the project version without any qualifier.
                // 2. If the branch is a release branch and branch name has multiple parts, the
                //    base version is the second part of the branch name.
                // 3. If the branch is a release or trunk branch and the project has a `baseVersion`
                //    property, the base version is the value of the `baseVersion` property.
                // 4. Otherwise, the base version is empty.
                val projectSemantic = RelaxedSemanticVersion(project.version.toString())
                val versionBase =
                    when {
                        extension.jenkinsMode && !projectSemantic.isEmpty() -> {
                            projectSemantic.withClearedQualifier().relaxedVersion()
                        }
                        isReleaseBranch && branchParts.size > 1 -> {
                            branchParts[1].trim()
                        }
                        (isReleaseBranch || isTrunkBranch) && project.properties.containsKey(BASE_VERSION_PROP) -> {
                            (project.properties[BASE_VERSION_PROP] as String).trim()
                        }
                        else -> {
                            ""
                        }
                    }
                val versionBaseSemantic = RelaxedSemanticVersion(versionBase)

                // Build the semantic and display versions together taking into account the base
                // version. If the base version is empty, the display and semantic versions are
                // computed based on the last tag information, otherwise they are computed using the
                // base version.
                val versionSemantic: RelaxedSemanticVersion
                val versionDisplay: String
                if (versionBase.isEmpty()) {
                    val lastTag = gitInfo.lastTag
                    val lastTagSemantic = RelaxedSemanticVersion(lastTag)
                    if (lastTagSemantic.isEmpty()) {
                        // The last tag is not semantically formatted, leave the semantic version
                        // empty.
                        versionSemantic = lastTagSemantic
                        if (lastTag.isNullOrEmpty()) {
                            // There is no last tag, the display version is the full version.
                            versionDisplay = versionFull
                        } else {
                            // The last tag is not empty, the display version is the last tag with
                            // the pre-release suffix appended.
                            versionDisplay = "$lastTag-$versionPreReleaseSuffix"
                        }
                    } else {
                        // The last tag is semantically formatted, set the pre-release suffix as the
                        // qualifier on the semantic version and use the relaxed semantic version as
                        // the display version.
                        versionSemantic = lastTagSemantic.withQualifier(versionPreReleaseSuffix)
                        versionDisplay = versionSemantic.relaxedVersion()
                    }
                } else if (gitInfo.shallow) {
                    // In case the repository has no history (shallow clone or check
                    // out), the last tags cannot be gotten and the display version
                    // cannot be computed correctly. The only special case is when the
                    // HEAD commit is exactly on a tag, and we can use it, in any other
                    // case, we can only start from the base information and add a
                    // snapshot information.
                    versionSemantic =
                        RelaxedSemanticVersion((gitInfo.tag ?: "$versionBase-SNAPSHOT") + versionDirtySuffix)
                    versionDisplay = versionSemantic.strictVersion()
                } else {
                    // Determine the next tag number, if in jenkins mode and the `BUILD_NUMBER`
                    // environment variable is set to an number, use it as the next tag number,
                    // otherwise build the next tag number based off the last tag.
                    val buildNumber =
                        if (!System.getenv(BUILD_NUMBER_ENV).isNullOrBlank()) {
                            System.getenv(BUILD_NUMBER_ENV).toIntOrNull()
                        } else {
                            null
                        }
                    val nextTagNumber =
                        if (extension.jenkinsMode && buildNumber != null) {
                            buildNumber
                        } else {
                            // Build a regular expression that will match all tags that should be
                            // considered when determining the last tag. The regular expression must
                            // also have a single capturing group that will extract the version
                            // segment that is to be incremented. If the base version is fully
                            // qualified, the last numeric segment of the matching tag should be
                            // captured. If the base version is a single number, the second segment
                            // of the matching tag should be captured.
                            var lastTagNumberPattern = "^$versionBase\\.(\\d+)\$"
                            if (versionBaseSemantic.qualifier().isNotEmpty()) {
                                val versionBaseStrict = versionBaseSemantic.strictVersion()
                                if (versionBaseStrict != versionBase) {
                                    lastTagNumberPattern = "^(?:$versionBase|$versionBaseStrict)\\.(\\d+)\$"
                                }
                            } else if (versionBaseSemantic.hasMajor() && !versionBaseSemantic.hasMinor()) {
                                val versionMajor = versionBaseSemantic.major()
                                lastTagNumberPattern = "^$versionMajor\\.(\\d+)(?:\\.\\d+)?\$"
                            }
                            val lastTagNumberRegex = lastTagNumberPattern.toRegex()
                            // Find the last tag that matches the expression
                            val lastTagName =
                                GitRepositoryInfo.getLastMatchingTagName(lastTagNumberRegex, tagNames).getOrDefault("")
                            // Extract last tag number from the last tag and increment it
                            lastTagNumberRegex.find(lastTagName)?.destructured?.let { (lastTagNumber) ->
                                lastTagNumber.toInt() + 1
                            } ?: 0
                        }
                    // When Jenkins mode is enabled on a non release branch, the semantic version is
                    // built by appending the branch id and the next tag number to the base version.
                    // Otherwise, the semantic version is built by appending the next tag number and
                    // dirty suffix to the base version.
                    if (!isReleaseBranch && extension.jenkinsMode) {
                        versionSemantic = RelaxedSemanticVersion("$versionBase-$branchId.$nextTagNumber")
                    } else {
                        // If the base version is strictly formatted as `major.minor.patch` without
                        // a qualifier, the next tag number should be appended as a qualifier,
                        // otherwise it should be appended as a standard version number.
                        val nextTagSeparator = if (versionBaseSemantic.isStrictNoQualifier()) "-" else "."
                        versionSemantic =
                            RelaxedSemanticVersion("$versionBase$nextTagSeparator$nextTagNumber$versionDirtySuffix")
                    }
                    // When jenkins mode is enabled, the display version is the relaxed semantic
                    // version, otherwise it is the strict semantic version.
                    versionDisplay =
                        if (extension.jenkinsMode) versionSemantic.relaxedVersion() else versionSemantic.strictVersion()
                }

                return VersionInfo(
                    "git",
                    gitInfo.branch,
                    branchType,
                    branchId,
                    gitInfo.commit!!,
                    gitInfo.abbreviated!!,
                    gitInfo.dateTime?.let { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) },
                    gitInfo.tag,
                    gitInfo.lastTag,
                    gitInfo.dirty,
                    gitInfo.shallow,
                    versionBase,
                    versionFull,
                    versionDisplay,
                    VersionNumber(
                        versionSemantic.major(),
                        versionSemantic.minor(),
                        versionSemantic.patch(),
                        versionSemantic.qualifier(),
                        versionSemantic.strictVersion()
                    )
                )
            }
        }

        private fun dirtyVersionSuffix(logger: Logger, gitInfo: GitRepositoryInfo, failOnDirty: Boolean): String {
            if (!gitInfo.dirty) {
                return ""
            }

            if (failOnDirty) {
                val status = gitInfo.status
                logger.warn("Git status:")
                mapOf(
                        "added" to status!!.added,
                        "changed" to status.changed,
                        "removed" to status.removed,
                        "missing" to status.missing,
                        "modified" to status.modified,
                        "conflicting" to status.conflicting,
                        "untracked" to status.untracked,
                        "untrackedFolders" to status.untrackedFolders
                    )
                    .forEach { (type: String, changes: Set<String?>?) ->
                        if (!changes.isNullOrEmpty()) {
                            logger.warn("$type [\n\t{}\n]", java.lang.String.join("\n\t", changes))
                        }
                    }
                throw GitDirtyException()
            }
            logger.warn("Git repository has un-staged or un-committed changes.")
            return "-dirty"
        }
    }

    override fun apply(project: Project) {
        // Create the extension and tasks
        val extension = project.extensions.create("versioning", VersioningExtension::class.java, project)
        project.tasks.register("versionDisplay", VersionDisplayTask::class.java)
        project.tasks.register("versionFile", VersionFileTask::class.java)

        // Set version only if we have info from git
        extension.info.let { info ->
            if (info.isNotEmpty()) {
                info.display?.let { v ->
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
