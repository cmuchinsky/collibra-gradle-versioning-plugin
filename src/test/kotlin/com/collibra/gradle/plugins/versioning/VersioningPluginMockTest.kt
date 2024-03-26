package com.collibra.gradle.plugins.versioning

import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.ZonedDateTime
import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VersioningPluginMockTest {
    companion object {
        private const val ABBREV_COMMIT_LEN = 7
        private const val ALPHA_FULL_COMMIT = "abcdefghijklmnopqrstuvwxyzabcdefghiklmno"
        private val ALPHA_ABBREV_COMMIT = ALPHA_FULL_COMMIT.take(ABBREV_COMMIT_LEN)
        private val ALPHA_COMMIT_PRE_RELEASE_SUFFIX = "sha-$ALPHA_ABBREV_COMMIT"
        private const val NUMERIC_FULL_COMMIT = "0123456789012345678901234567890123456789"
        private val NUMERIC_ABBREV_COMMIT = NUMERIC_FULL_COMMIT.take(ABBREV_COMMIT_LEN)
        private val NUMERIC_COMMIT_PRE_RELEASE_SUFFIX = "sha-$NUMERIC_ABBREV_COMMIT"

        private fun projectWithPlugin(
            gitInfo: GitRepositoryInfo? = null,
            props: Map<out String, Any> = emptyMap()
        ): Project {
            val project: Project = ProjectBuilder.builder().build()
            props.entries.forEach { property -> project.extra.set(property.key, property.value) }
            val git = mockkClass(Git::class)
            every { git.close() } returns Unit
            every { GitRepositoryInfo.checkGitDirectory(any(), any()) } returns true
            every { GitRepositoryInfo.openGit(any(), any()) } returns git
            gitInfo?.let { every { GitRepositoryInfo.getBranchName(any(), any()) } returns it.branch }
            every { GitRepositoryInfo.getTagRefs(any()) } returns mapOf()
            val tagNames = GitRepositoryInfo.withTagEquivalents(gitInfo?.lastTag?.let { listOf(it) } ?: listOf())
            every { GitRepositoryInfo.getTagNames(any(), any(), any()) } returns tagNames
            gitInfo?.let { every { GitRepositoryInfo.buildInfo(any(), any(), any(), any()) } returns it }
            project.pluginManager.apply(VersioningPlugin::class.java)
            return project
        }

        private fun buildGitInfo(branch: String, lastTag: String?, commit: String): GitRepositoryInfo {
            return GitRepositoryInfo(
                branch,
                commit,
                commit.take(ABBREV_COMMIT_LEN),
                ZonedDateTime.now(),
                null,
                lastTag,
                null,
                false,
                false
            )
        }

        private fun buildVersionInfo(
            branch: String,
            lastTag: String?,
            baseVersion: String? = null,
            commit: String = ALPHA_FULL_COMMIT
        ): VersionInfo {
            val project =
                projectWithPlugin(
                    buildGitInfo(branch, lastTag, commit),
                    baseVersion?.let { mapOf("baseVersion" to it) } ?: emptyMap()
                )
            val info = project.extensions.getByType(VersioningExtension::class.java).info
            println(info)
            assertEquals(info.display, project.version.toString())
            return info
        }
    }

    @BeforeEach
    fun beforeEach() {
        mockkObject(GitRepositoryInfo)
    }

    @AfterEach
    fun afterEach() {
        unmockkObject(GitRepositoryInfo)
    }

    @Test
    fun `Test release branches`() {
        var info = buildVersionInfo("pre/0", null)
        assertEquals("0.0.0", info.display)
        info = buildVersionInfo("pre/1", "")
        assertEquals("1.0.0", info.display)
        info = buildVersionInfo("pre/2", "2")
        assertEquals("2.1.0", info.display)
        info = buildVersionInfo("pre/2", "2.0")
        assertEquals("2.1.0", info.display)
        info = buildVersionInfo("pre/2", "2.2.0")
        assertEquals("2.3.0", info.display)
        info = buildVersionInfo("pre/2", "3.0")
        assertEquals("2.0.0", info.display)
        info = buildVersionInfo("release/0.0", null)
        assertEquals("0.0.0", info.display)
        info = buildVersionInfo("release/1.0", "")
        assertEquals("1.0.0", info.display)
        info = buildVersionInfo("release/2.0", "2.0")
        assertEquals("2.0.1", info.display)
        info = buildVersionInfo("release/2.0", "2.0.0")
        assertEquals("2.0.1", info.display)
        info = buildVersionInfo("release/2.0", "3.0.0")
        assertEquals("2.0.0", info.display)
        info = buildVersionInfo("release/2024.04", "2024.04.0-100")
        assertEquals("2024.4.0", info.display)
    }

    @Test
    fun `Test trunk branches`() {
        var info = buildVersionInfo("main", null, " 00 ")
        assertEquals("0.0.0", info.display)
        info = buildVersionInfo("main", "", "1")
        assertEquals("1.0.0", info.display)
        info = buildVersionInfo("main", "2", "2")
        assertEquals("2.1.0", info.display)
        info = buildVersionInfo("main", "2.0", "2")
        assertEquals("2.1.0", info.display)
        info = buildVersionInfo("main", "2.0.0", "2")
        assertEquals("2.1.0", info.display)
        info = buildVersionInfo("main", "2", "2.0")
        assertEquals("2.0.1", info.display)
        info = buildVersionInfo("main", "2.0", "2.0")
        assertEquals("2.0.1", info.display)
        info = buildVersionInfo("main", "2.0.0", "2.0")
        assertEquals("2.0.1", info.display)
        info = buildVersionInfo("main", "3", "2.0")
        assertEquals("2.0.0", info.display)
        info = buildVersionInfo("main", "3.0", "2.0")
        assertEquals("2.0.0", info.display)
        info = buildVersionInfo("main", "3.0.0", "2.0")
        assertEquals("2.0.0", info.display)
        info = buildVersionInfo("main", "NaN", "NaN")
        assertEquals("0.0.0", info.display)
        info = buildVersionInfo("main", "2024.04.0-100", "2024.04.0-SNAPSHOT")
        assertEquals("2024.4.0-SNAPSHOT.0", info.display)
    }

    @Test
    fun `Test trunk branches without base`() {
        var info = buildVersionInfo("main", null)
        assertEquals("main-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("main", "")
        assertEquals("main-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("feature/DEV-00000", "1.0")
        assertEquals("1.0-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("bug/2.0.0", "1.0.0", commit = NUMERIC_FULL_COMMIT)
        assertEquals("1.0.0-$NUMERIC_COMMIT_PRE_RELEASE_SUFFIX", info.display)
    }

    @Test
    fun `Test non auto-versioned branches`() {
        var info = buildVersionInfo("feature/DEV-00000", null)
        assertEquals("feature-DEV-00000-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("bug/DEV-00000", "")
        assertEquals("bug-DEV-00000-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("feature/1.0", "1.0")
        assertEquals("1.0-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("feature/2.0.0", "1.0.0")
        assertEquals("1.0.0-$ALPHA_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("", "2.2.2", commit = NUMERIC_FULL_COMMIT)
        assertEquals("2.2.2-$NUMERIC_COMMIT_PRE_RELEASE_SUFFIX", info.display)
        info = buildVersionInfo("", "NaN", commit = NUMERIC_FULL_COMMIT)
        assertEquals("NaN-$NUMERIC_COMMIT_PRE_RELEASE_SUFFIX", info.display)
    }
}
