package com.collibra.gradle.plugins.versioning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VersioningPluginJenkinsModeTest {
    @Test
    fun `Test jenkins branch release - no previous tag`() {
        GitRepository().use { repo ->
            // given
            with(repo) {
                (1..4).forEach { commit(it) }
                branch("release/2024.05")
                commit(5)
            }
            val head = repo.commitLookup("Commit 5")
            val headAbbreviated = repo.commitLookup("Commit 5", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.version = "2024.05.0-SNAPSHOT"
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)
            extension.jenkinsMode = true

            // then
            assertEquals(
                VersionInfo(
                    "git",
                    "release/2024.05",
                    "release",
                    "release-2024.05",
                    head,
                    headAbbreviated,
                    time,
                    null,
                    null,
                    false,
                    false,
                    "2024.05.0",
                    "release-2024.05-sha-${headAbbreviated}",
                    "2024.05.0-12345",
                    VersionNumber(2024, 5, 0, "-12345", "2024.5.0-12345")
                ),
                extension.info
            )
        }
    }

    @Test
    fun `Test jenkins branch feature - no previous tag`() {
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
            project.version = "2024.05.0-SNAPSHOT"
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)
            extension.jenkinsMode = true

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
                    "2024.05.0",
                    "feature-123-great-sha-${headAbbreviated}",
                    "2024.05.0-feature-123-great.12345",
                    VersionNumber(2024, 5, 0, "-feature-123-great.12345", "2024.5.0-feature-123-great.12345")
                ),
                extension.info
            )
        }
    }
}
