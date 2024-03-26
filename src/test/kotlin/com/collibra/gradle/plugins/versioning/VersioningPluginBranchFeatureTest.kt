package com.collibra.gradle.plugins.versioning

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VersioningPluginBranchFeatureTest {
    @Test
    fun `Test branch feature - no previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("feature/123-great")
                commit(5)
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "feature/123-great",
                    "feature",
                    "feature-123-great",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "",
                    "feature-123-great-sha-${headAbbreviated}",
                    "feature-123-great-sha-${headAbbreviated}",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch feature - dirty repo`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("feature/123-great")
                commit(5)
                // Modify an existing tracked file
                Files.writeString(dir.toPath().resolve("file5"), "Add some content", StandardOpenOption.APPEND)
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "feature/123-great",
                    "feature",
                    "feature-123-great",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    true,
                    false,
                    "",
                    "feature-123-great-sha-${headAbbreviated}-dirty",
                    "feature-123-great-sha-${headAbbreviated}-dirty",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch feature - dirty index`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("feature/123-great")
                commit(5)
                // Add a new file
                Files.writeString(dir.toPath().resolve("test.text"), "test")
                add("test.text")
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "feature/123-great",
                    "feature",
                    "feature-123-great",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    true,
                    false,
                    "",
                    "feature-123-great-sha-${headAbbreviated}-dirty",
                    "feature-123-great-sha-${headAbbreviated}-dirty",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch feature - ignored files - not dirty`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("feature/123-great")
                commit(5)
                // Ignore file
                Files.writeString(dir.toPath().resolve(".gitignore"), "test.txt")
                add(".gitignore")
                commit(6)
                // Add a new file
                Files.writeString(dir.toPath().resolve("test.text"), "test")
            }
            val head = repo.commitLookup("Commit 6")
            val headAbbreviated = repo.commitLookup("Commit 6", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "feature/123-great",
                    "feature",
                    "feature-123-great",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "",
                    "feature-123-great-sha-${headAbbreviated}",
                    "feature-123-great-sha-${headAbbreviated}",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch feature - env GIT_TEST_BRANCH`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("feature/123-great")
                commit(5)
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)
            extension.branchEnv += "TEST_BRANCH"

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "feature/456-cute",
                    "feature",
                    "feature-456-cute",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "",
                    "feature-456-cute-sha-${headAbbreviated}",
                    "feature-456-cute-sha-${headAbbreviated}",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }
}
