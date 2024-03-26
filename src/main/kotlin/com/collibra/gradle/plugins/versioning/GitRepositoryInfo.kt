package com.collibra.gradle.plugins.versioning

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project

data class GitRepositoryInfo(
    val branch: String,
    val commit: String?,
    val abbreviated: String?,
    val dateTime: ZonedDateTime?,
    val tag: String?,
    val lastTag: String?,
    val status: Status?,
    val dirty: Boolean,
    val shallow: Boolean
) {
    companion object {
        private val LAST_NUMBER_REGEX = "(\\d+)\$".toRegex()
        private val DESCRIBE_LONG_REGEX = "^(.*)-(\\d+)-g([0-9a-f]+)\$".toRegex()

        fun checkGitDirectory(project: Project, extension: VersioningExtension): Boolean {
            return project.rootProject.file(".git").exists() ||
                project.file(".git").exists() ||
                (extension.gitRepoRootDir != null && File(extension.gitRepoRootDir, ".git").exists())
        }

        fun openGit(project: Project, extension: VersioningExtension): Git {
            val gitDirectory = extension.gitRepoRootDir?.let { File(it) } ?: project.projectDir
            val gitRepositoryBuilder = FileRepositoryBuilder()
            gitRepositoryBuilder.readEnvironment()
            gitRepositoryBuilder.findGitDir(gitDirectory)
            return Git(gitRepositoryBuilder.build())
        }

        fun getBranchName(git: Git, extension: VersioningExtension): String? {
            for (ev in extension.branchEnv) {
                // Check Environment
                if (System.getenv(ev) != null) {
                    return System.getenv(ev)
                }
            }
            // Check Repository
            return git.repository.exactRef(Constants.HEAD)?.target?.let { Repository.shortenRefName(it.name) }
        }

        fun getTagRefs(git: Git): Map<ObjectId, List<String>> {
            return git.repository.refDatabase
                .getRefsByPrefix(Constants.R_TAGS)
                .groupBy { git.repository.refDatabase.peel(it).peeledObjectId ?: it.objectId }
                .mapValues { it.value.mapNotNull { ref -> ref?.name?.substring(Constants.R_TAGS.length) } }
        }

        fun withTagEquivalents(tagNames: List<String>): List<String> {
            // Add equivalent tags for <major> and <major.minor> only tags
            return tagNames.flatMap { RelaxedSemanticVersion(it).equivalents().ifEmpty { listOf(it) } }.distinct()
        }

        fun getTagNames(git: Git, tagRefs: Map<ObjectId, List<String>>, branch: String): List<String> {
            val log = git.log()
            git.repository.resolve(branch)?.let { log.add(it) }
            return withTagEquivalents(log.call().mapNotNull { tagRefs[it.id] }.flatten())
        }

        fun getLastMatchingTagName(tagNumberRegex: Regex, tagNames: List<String>): Optional<String> {
            // filter using the pattern and sort desc by tag parts
            return tagNames
                .stream()
                .filter { tagNumberRegex.containsMatchIn(it) }
                .map {
                    tagNumberRegex.find(it)!!.destructured.let { (tagNumberMatch) ->
                        Triple(it, it.substringBeforeLast(tagNumberMatch), tagNumberMatch.toInt())
                    }
                }
                .sorted { tagPartsA, tagPartsB ->
                    tagPartsB.second.compareTo(tagPartsA.second).let {
                        if (it != 0) it else tagPartsB.third.compareTo(tagPartsA.third)
                    }
                }
                .map { it.first }
                .findFirst()
        }

        fun buildInfo(
            git: Git,
            branch: String,
            tagRefs: Map<ObjectId, List<String>>,
            tagNames: List<String>
        ): GitRepositoryInfo {
            // Commit Info
            val commits = git.log().setMaxCount(1).call().toList()
            if (commits.isEmpty()) {
                throw GradleException("No commit available in the repository - cannot compute version")
            }
            val lastCommit = commits[0]
            // Commit Hash - full
            val commit = ObjectId.toString(lastCommit)
            // Commit Hash - short
            val abbreviated = git.repository.newObjectReader().use { it.abbreviate(lastCommit).name() }
            // Commit DateTime
            val dateTime =
                ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(lastCommit.commitTime.toLong()),
                    lastCommit.committerIdent.timeZone?.toZoneId() ?: ZoneOffset.UTC
                )

            // Shallow Repository
            val shallow = lastCommit.parentCount == 0

            // Current Tag
            val tag: String?
            if (shallow) {
                // If we're on a tag, we can use it directly
                tag = tagRefs[git.repository.resolve(Constants.HEAD)]?.last()
            } else {
                val described = git.describe().setTags(true).setLong(true).call()
                if (described != null) {
                    // The format returned by the long version of the `describe` command is:
                    // <tag>-<number>-<commit>
                    val describeResult = DESCRIBE_LONG_REGEX.find(described)
                    if (describeResult != null) {
                        tag =
                            describeResult.destructured.let { (tagStr, tagCount, _) ->
                                if (tagCount.toInt() == 0) tagStr else null
                            }
                    } else {
                        throw GradleException("Cannot get parse description of current commit: $described")
                    }
                } else {
                    // Nothing returned - it means there is no previous tag
                    tag = null
                }
            }

            // Last Tag
            val lastTag = getLastMatchingTagName(LAST_NUMBER_REGEX, tagNames).getOrNull()

            // Status
            val status = git.status().call()

            // Git Information
            return GitRepositoryInfo(
                branch,
                commit,
                abbreviated,
                dateTime,
                tag,
                lastTag,
                status,
                status.hasUncommittedChanges(),
                shallow
            )
        }
    }
}
