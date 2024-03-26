package com.collibra.gradle.plugins.versioning

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VersioningPluginBranchTrunkTest {
    @Test
    fun `Test branch trunk - no previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "main",
                    "main",
                    "main",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "",
                    "main-sha-${headAbbreviated}",
                    "main-sha-${headAbbreviated}",
                    VersionNumber(0, 0, 0, "", "0.0.0")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch trunk - with previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                tag("2.0.2")
                (5..6).forEach { commit(it) }
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
                    "main",
                    "main",
                    "main",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    "2.0.2",
                    false,
                    false,
                    "",
                    "main-sha-${headAbbreviated}",
                    "2.0.2-sha-${headAbbreviated}",
                    VersionNumber(2, 0, 2, "-sha-${headAbbreviated}", "2.0.2-sha-${headAbbreviated}")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch trunk - on a tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..6).forEach { commit(it) }
                tag("2.0.2")
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
                    "main",
                    "main",
                    "main",
                    head,
                    headAbbreviated,
                    time,
                    "2.0.2",
                    "2.0.2",
                    false,
                    false,
                    "",
                    "main-sha-${headAbbreviated}",
                    "2.0.2-sha-${headAbbreviated}",
                    VersionNumber(2, 0, 2, "-sha-${headAbbreviated}", "2.0.2-sha-${headAbbreviated}")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test branch trunk - shallow history on a tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                tag("2.0.2")
            }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
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
                        "main",
                        "main",
                        "main",
                        head,
                        headAbbreviated,
                        time,
                        "2.0.2",
                        "2.0.2",
                        false,
                        true,
                        "",
                        "main-sha-${headAbbreviated}",
                        "2.0.2-sha-${headAbbreviated}",
                        VersionNumber(2, 0, 2, "-sha-${headAbbreviated}", "2.0.2-sha-${headAbbreviated}")
                    ),
                    extension.info
                )
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }

    @Test
    fun `Test branch trunk - shallow history not on a tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
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
                        "main",
                        "main",
                        "main",
                        head,
                        headAbbreviated,
                        time,
                        null,
                        null,
                        false,
                        true,
                        "",
                        "main-sha-${headAbbreviated}",
                        "main-sha-${headAbbreviated}",
                        VersionNumber(0, 0, 0, "", "0.0.0")
                    ),
                    extension.info
                )
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }
}
