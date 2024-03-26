package com.collibra.gradle.plugins.versioning

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class VersioningPluginBranchReleaseTest {
    @Test
    fun `Test branch release - no previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
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
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}",
                    "2.0.0",
                    VersionNumber(2, 0, 0, "", "2.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.2")
                commit(6)
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
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.2",
                    false,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}",
                    "2.0.3",
                    VersionNumber(2, 0, 3, "", "2.0.3")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag, two tags on commit`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                tag("2.0.2")
                tag("2.0.3")
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
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.3",
                    false,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}",
                    "2.0.4",
                    VersionNumber(2, 0, 4, "", "2.0.4")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag, chronological order of tags must be taken into account`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/1.3")
                commit(5)
                tag("1.2.16")
                Thread.sleep(1000)
                commit(6)
                tag("1.3.11")
                commit(7)
            }
            val head = repo.commitLookup("Commit 7")
            val headAbbreviated = repo.commitLookup("Commit 7", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/1.3",
                    "release",
                    "release-1.3",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "1.3.11",
                    false,
                    false,
                    "1.3",
                    "release-1.3-sha-${headAbbreviated}",
                    "1.3.12",
                    VersionNumber(1, 3, 12, "", "1.3.12")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag alpha`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0-alpha")
                commit(5)
                tag("2.0-alpha.2")
                commit(6)
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
                    "release/2.0-alpha",
                    "release",
                    "release-2.0-alpha",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0-alpha.2",
                    false,
                    false,
                    "2.0-alpha",
                    "release-2.0-alpha-sha-${headAbbreviated}",
                    "2.0.0-alpha.3",
                    VersionNumber(2, 0, 0, "-alpha.3", "2.0.0-alpha.3")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tags alpha, older tag must be taken into account`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/3.0-alpha")
                commit(5)
                tag("3.0.0-alpha.0")
                Thread.sleep(1000)
                commit(6)
                tag("3.0.0-alpha.1")
                commit(7)
            }
            val head = repo.commitLookup("Commit 7")
            val headAbbreviated = repo.commitLookup("Commit 7", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/3.0-alpha",
                    "release",
                    "release-3.0-alpha",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "3.0.0-alpha.1",
                    false,
                    false,
                    "3.0-alpha",
                    "release-3.0-alpha-sha-${headAbbreviated}",
                    "3.0.0-alpha.2",
                    VersionNumber(3, 0, 0, "-alpha.2", "3.0.0-alpha.2")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tags alpha, chronological order of tags must be taken into account`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/3.0-alpha")
                commit(5)
                tag("3.0-alpha.9")
                Thread.sleep(1000)
                commit(6)
                tag("3.0-alpha.10")
                commit(7)
            }
            val head = repo.commitLookup("Commit 7")
            val headAbbreviated = repo.commitLookup("Commit 7", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/3.0-alpha",
                    "release",
                    "release-3.0-alpha",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "3.0-alpha.10",
                    false,
                    false,
                    "3.0-alpha",
                    "release-3.0-alpha-sha-${headAbbreviated}",
                    "3.0.0-alpha.11",
                    VersionNumber(3, 0, 0, "-alpha.11", "3.0.0-alpha.11")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tags alpha, chronological order of tags must be taken into account - 2`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/3.0-alpha")
                commit(5)
                tag("3.0-alpha.19")
                Thread.sleep(1000)
                commit(6)
                tag("3.0.0-alpha.20")
                commit(7)
            }
            val head = repo.commitLookup("Commit 7")
            val headAbbreviated = repo.commitLookup("Commit 7", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/3.0-alpha",
                    "release",
                    "release-3.0-alpha",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "3.0.0-alpha.20",
                    false,
                    false,
                    "3.0-alpha",
                    "release-3.0-alpha-sha-${headAbbreviated}",
                    "3.0.0-alpha.21",
                    VersionNumber(3, 0, 0, "-alpha.21", "3.0.0-alpha.21")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag on different branches`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.2")
                branch("release/2.1")
                commit(6)
                tag("2.0.10")
                checkout("release/2.0")
                commit(7)
            }
            val head = repo.commitLookup("Commit 7")
            val headAbbreviated = repo.commitLookup("Commit 7", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.2",
                    false,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}",
                    "2.0.3",
                    VersionNumber(2, 0, 3, "", "2.0.3")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - with previous tag with two final digits`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.10")
                commit(6)
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
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.10",
                    false,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}",
                    "2.0.11",
                    VersionNumber(2, 0, 11, "", "2.0.11")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - dirty`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.2")
                commit(6)
                // Modify an existing tracked file
                Files.writeString(dir.toPath().resolve("file5"), "Add some content", StandardOpenOption.APPEND)
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
                    "release/2.0",
                    "release",
                    "release-2.0",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.2",
                    true,
                    false,
                    "2.0",
                    "release-2.0-sha-${headAbbreviated}-dirty",
                    "2.0.3-dirty",
                    VersionNumber(2, 0, 3, "-dirty", "2.0.3-dirty")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch release - dirty - fail`() {
        assertThrows(GitDirtyException::class.java) {
            GitRepository().use { repo ->
                // given
                with(repo) {
                    (1..4).forEach { commit(it) }
                    branch("release/2.0")
                    commit(5)
                    tag("2.0.2")
                    commit(6)
                    // Modify an existing tracked file
                    Files.writeString(dir.toPath().resolve("file5"), "Add some content", StandardOpenOption.APPEND)
                }

                // when
                val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
                project.pluginManager.apply(VersioningPlugin::class.java)
                val extension = project.extensions.getByType(VersioningExtension::class.java)
                extension.dirtyFailOnReleases = true
                extension.info
            }
        }
    }

    @Test
    fun `Test branch release - shallow history on a tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.2")
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val projectDir = Files.createTempDirectory("project")
            try {
                ProcessBuilder("git", "clone", "--depth", "1", "file://${repo.dir.absolutePath}", ".")
                    .directory(projectDir.toFile())
                    .start()
                    .waitFor(2000L, TimeUnit.MILLISECONDS)
                val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
                project.pluginManager.apply(VersioningPlugin::class.java)
                val extension = project.extensions.getByType(VersioningExtension::class.java)

                // then
                assertEquals(
                    VersionInfo(
                        "git",
                        "release/2.0",
                        "release",
                        "release-2.0",
                        head,
                        headAbbreviated,
                        time,
                        "2.0.2",
                        "2.0.2",
                        false,
                        true,
                        "2.0",
                        "release-2.0-sha-${headAbbreviated}",
                        "2.0.2",
                        VersionNumber(2, 0, 2, "", "2.0.2")
                    ),
                    extension.info
                )
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }

    @Test
    fun `Test branch release - shallow history not on a tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2.0")
                commit(5)
                tag("2.0.2")
                commit(6)
            }
            val head = repo.commitLookup("Commit 6")
            val headAbbreviated = repo.commitLookup("Commit 6", true)
            val time = repo.commitTime(head)

            // when
            val projectDir = Files.createTempDirectory("project")
            try {
                ProcessBuilder("git", "clone", "--depth", "1", "file://${repo.dir.absolutePath}", ".")
                    .directory(projectDir.toFile())
                    .start()
                    .waitFor(2000L, TimeUnit.MILLISECONDS)
                val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
                project.pluginManager.apply(VersioningPlugin::class.java)
                val extension = project.extensions.getByType(VersioningExtension::class.java)

                // then
                assertEquals(
                    VersionInfo(
                        "git",
                        "release/2.0",
                        "release",
                        "release-2.0",
                        head,
                        headAbbreviated,
                        time,
                        null,
                        null,
                        false,
                        true,
                        "2.0",
                        "release-2.0-sha-${headAbbreviated}",
                        "2.0.0-SNAPSHOT",
                        VersionNumber(2, 0, 0, "-SNAPSHOT", "2.0.0-SNAPSHOT")
                    ),
                    extension.info
                )
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }
}
