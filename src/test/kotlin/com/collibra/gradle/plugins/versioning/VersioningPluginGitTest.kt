package com.collibra.gradle.plugins.versioning

import java.io.File
import java.nio.file.Files
import org.eclipse.jgit.api.Git
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VersioningPluginGitTest {
    @Test
    fun `Test git not present`() {
        // when
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(VersioningPlugin::class.java)
        val extension = project.extensions.getByType(VersioningExtension::class.java)

        // then
        assertNotNull(extension.info)
        assertTrue(extension.info.isEmpty())
    }

    @Test
    fun `Test git detached HEAD`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val commit3 = repo.commitLookup("Commit 3")
            val commit3Abbreviated = repo.commitLookup("Commit 3", true)
            val time = repo.commitTime(commit3)

            // when
            val projectDir = Files.createTempDirectory("project")
            try {
                Git.cloneRepository()
                    .setURI(repo.dir.toURI().toString())
                    .setDirectory(projectDir.toFile())
                    .call()
                    .use { jgit ->
                        jgit.checkout().setName(commit3).call() // Detached HEAD checkout
                        val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
                        project.pluginManager.apply(VersioningPlugin::class.java)
                        val extension = project.extensions.getByType(VersioningExtension::class.java)

                        // then
                        assertEquals(
                            VersionInfo(
                                "git",
                                "HEAD",
                                "HEAD",
                                "HEAD",
                                commit3,
                                commit3Abbreviated,
                                time,
                                null,
                                null,
                                false,
                                false,
                                "",
                                "HEAD-sha-${commit3Abbreviated}",
                                "HEAD-sha-${commit3Abbreviated}",
                                VersionNumber(0, 0, 0, "", "0.0.0")
                            ),
                            extension.info
                        )
                    }
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }

    @Test
    fun `Test git custom directory`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            repo.branch("release/2.0")
            repo.tag("2.0.2")
            repo.commit(5)
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val projectDir = Files.createTempDirectory("project")
            try {
                val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
                project.pluginManager.apply(VersioningPlugin::class.java)
                val extension = project.extensions.getByType(VersioningExtension::class.java)
                extension.gitRepoRootDir = repo.dir.absolutePath

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
            } finally {
                projectDir.toFile().deleteRecursively()
            }
        }
    }

    @Test
    fun `Test git gradle subproject`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            val subProjectDir = File(repo.dir, "sub")
            subProjectDir.mkdirs()
            subProjectDir.deleteOnExit()
            val subProject = ProjectBuilder.builder().withParent(project).withProjectDir(subProjectDir).build()
            subProject.pluginManager.apply(VersioningPlugin::class.java)
            val extension = subProject.extensions.getByType(VersioningExtension::class.java)

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
}
