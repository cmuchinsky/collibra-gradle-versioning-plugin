package com.collibra.gradle.plugins.versioning.git

import com.collibra.gradle.plugins.versioning.SCMInfo
import com.collibra.gradle.plugins.versioning.VersioningExtension
import java.io.File
import java.util.regex.Pattern
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Status
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.gradle.api.GradleException
import org.gradle.api.Project

internal object GitInfoService {
    fun getInfo(project: Project, extension: VersioningExtension): SCMInfo {
        // Is Git enabled?
        val hasGit =
            (project.rootProject.file(".git").exists() ||
                project.file(".git").exists() ||
                (extension.gitRepoRootDir != null && File(extension.gitRepoRootDir, ".git").exists()))
        // No Git information
        if (!hasGit) {
            return SCMInfo.empty()
        } else {
            // Git directory
            val gitDir = getGitDirectory(extension, project)

            Grgit.open(mapOf("currentDir" to gitDir)).use { grgit ->
                // Check passed in environment variable list
                var branch: String? = null
                for (ev in extension.branchEnv) {
                    if (System.getenv(ev) != null) {
                        branch = System.getenv(ev)
                        break
                    }
                }

                // Gets the commit info (full hash)
                val commits = grgit.log(mapOf("maxCommits" to 1))
                if (commits.isEmpty()) {
                    throw GradleException("No commit available in the repository - cannot compute version")
                }

                val lastCommit = commits[0]
                // Full commit hash
                val commit = lastCommit.id
                // Gets the current commit (short hash)
                val abbreviated = lastCommit.abbreviatedId
                // Gets the time the commit was created
                val dateTime = lastCommit.dateTime

                // Git always provides a time so no null-handling required

                // Is the repository shallow?
                val shallow = lastCommit.parentIds.isEmpty()

                // Gets the current tag, if any
                val tag: String?
                // Cannot use the `describe` command if the repository is shallow
                if (shallow) {
                    // Map of tags
                    val tags: MutableMap<ObjectId?, Ref> = HashMap()

                    val gitRepository = grgit.repository.jgit.repository

                    for (r in gitRepository.refDatabase.getRefsByPrefix(Constants.R_TAGS)) {
                        var key = gitRepository.refDatabase.peel(r).peeledObjectId
                        if (key == null) key = r.objectId
                        tags[key] = r
                    }

                    // If we're on a tag, we can use it directly
                    val lucky = tags[gitRepository.resolve(Constants.HEAD)]
                    tag = lucky?.name?.substring(Constants.R_TAGS.length)
                } else {
                    val described = grgit.repository.jgit.describe().setTags(true).setLong(true).call()
                    if (described != null) {
                        // The format returned by the long version of the `describe` command is:
                        // <tag>-<number>-<commit>
                        val m = Pattern.compile("^(.*)-(\\d+)-g([0-9a-f]+)$").matcher(described)
                        if (m.matches()) {
                            val count = m.group(2).toInt()
                            tag =
                                if (count == 0) {
                                    // We're on a tag
                                    m.group(1)
                                } else {
                                    // No tag
                                    null
                                }
                        } else {
                            throw GradleException("Cannot get parse description of current commit: $described")
                        }
                    } else {
                        // Nothing returned - it means there is no previous tag
                        tag = null
                    }
                }

                // Last tag
                val lastTag =
                    filterAndSortTags(
                            extension.lastTagPattern.toRegex(),
                            grgit.tag.list().stream().map { it.name.trim() }
                        )
                        .findFirst()
                        .getOrNull()

                // Returns the information
                val status = grgit.status()
                return SCMInfo(
                    branch ?: grgit.branch.current().name,
                    commit,
                    abbreviated,
                    dateTime,
                    tag,
                    lastTag,
                    status,
                    isDirty(status),
                    shallow
                )
            }
        }
    }

    fun filterAndSortTags(tagPatternRegex: Regex, tags: Stream<String>): Stream<String> {
        // filter using the pattern and sort by desc version
        return tags
            .filter { tagPatternRegex.containsMatchIn(it) }
            .sorted { a, b -> tagOrder(tagPatternRegex, b).compareTo(tagOrder(tagPatternRegex, a)) }
    }

    fun getLastTag(project: Project, extension: VersioningExtension, tagPatternRegex: Regex?): String? {
        Grgit.open(mapOf("currentDir" to getGitDirectory(extension, project))).use { grgit ->
            val tagStream = grgit.tag.list().stream().map { it.name.trim() }
            return (tagPatternRegex?.let { filterAndSortTags(it, tagStream) } ?: tagStream).findFirst().getOrNull()
        }
    }

    @JvmStatic
    fun isDirty(status: Status): Boolean {
        return status.staged.allChanges.isNotEmpty() ||
            !status.unstaged.allChanges.stream().allMatch { it: String -> it.startsWith("userHome/") }
    }

    private fun getGitDirectory(extension: VersioningExtension, project: Project): File {
        return extension.gitRepoRootDir?.let { File(it) } ?: project.projectDir
    }

    private fun tagOrder(tagPatternRegex: Regex, tagName: String): Int {
        tagPatternRegex.find(tagName)?.let { result ->
            require(result.groupValues.isNotEmpty()) {
                "Tag pattern is expected to have at least one number grouping instruction: $tagPatternRegex"
            }
            return result.groupValues[1].toInt()
        } ?: throw IllegalStateException("Tag $tagName should have matched $tagPatternRegex")
    }
}
