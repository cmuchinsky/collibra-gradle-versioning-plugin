package com.collibra.gradle.plugins.versioning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VersionFileTaskTest {
    @Test
    fun `Test tasks versionFile is present`() {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(VersioningPlugin::class.java)

        // when
        val tasks = project.tasks.withType(VersionFileTask::class.java)

        // then
        assertNotNull(tasks)
        assertFalse(tasks.isEmpty())
    }

    @Test
    fun `Test task versionFile executes - defaults`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val task = project.tasks.getByName("versionFile")
            task.actions.forEach { it.execute(task) }

            // then
            val file = project.layout.buildDirectory.file("version.properties").get().asFile
            assertTrue(file.exists())
            assertEquals(
                """
                VERSION_BUILD=$headAbbreviated
                VERSION_BRANCH=main
                VERSION_BASE=
                VERSION_BRANCHID=main
                VERSION_BRANCHTYPE=main
                VERSION_COMMIT=$head
                VERSION_GRADLE=main-sha-$headAbbreviated
                VERSION_DISPLAY=main-sha-$headAbbreviated
                VERSION_FULL=main-sha-$headAbbreviated
                VERSION_SCM=git
                VERSION_TAG=
                VERSION_LAST_TAG=
                VERSION_DIRTY=false
                VERSION_VERSIONCODE=0
                VERSION_MAJOR=0
                VERSION_MINOR=0
                VERSION_PATCH=0
                VERSION_QUALIFIER=
                VERSION_TIME=$time
                """
                    .trimIndent(),
                file.readText().trim()
            )
        }
    }

    @Test
    fun `Test task versionFile executes - project version`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.version = "0.0.1"
            project.pluginManager.apply(VersioningPlugin::class.java)
            val task = project.tasks.getByName("versionFile")
            task.actions.forEach { it.execute(task) }

            // then
            val file = project.layout.buildDirectory.file("version.properties").get().asFile
            assertTrue(file.exists())
            assertEquals(
                """
                VERSION_BUILD=$headAbbreviated
                VERSION_BRANCH=main
                VERSION_BASE=
                VERSION_BRANCHID=main
                VERSION_BRANCHTYPE=main
                VERSION_COMMIT=$head
                VERSION_GRADLE=0.0.1
                VERSION_DISPLAY=main-sha-$headAbbreviated
                VERSION_FULL=main-sha-$headAbbreviated
                VERSION_SCM=git
                VERSION_TAG=
                VERSION_LAST_TAG=
                VERSION_DIRTY=false
                VERSION_VERSIONCODE=0
                VERSION_MAJOR=0
                VERSION_MINOR=0
                VERSION_PATCH=0
                VERSION_QUALIFIER=
                VERSION_TIME=$time
                """
                    .trimIndent(),
                file.readText().trim()
            )
        }
    }

    @Test
    fun `Test task versionFile executes - custom prefix`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val task = project.tasks.getByName("versionFile") as VersionFileTask
            task.prefix.set("CUSTOM_")
            task.actions.forEach { it.execute(task) }

            // then
            val file = project.layout.buildDirectory.file("version.properties").get().asFile
            assertTrue(file.exists())
            assertEquals(
                """
                CUSTOM_BUILD=$headAbbreviated
                CUSTOM_BRANCH=main
                CUSTOM_BASE=
                CUSTOM_BRANCHID=main
                CUSTOM_BRANCHTYPE=main
                CUSTOM_COMMIT=$head
                CUSTOM_GRADLE=main-sha-$headAbbreviated
                CUSTOM_DISPLAY=main-sha-$headAbbreviated
                CUSTOM_FULL=main-sha-$headAbbreviated
                CUSTOM_SCM=git
                CUSTOM_TAG=
                CUSTOM_LAST_TAG=
                CUSTOM_DIRTY=false
                CUSTOM_VERSIONCODE=0
                CUSTOM_MAJOR=0
                CUSTOM_MINOR=0
                CUSTOM_PATCH=0
                CUSTOM_QUALIFIER=
                CUSTOM_TIME=$time
                """
                    .trimIndent(),
                file.readText().trim()
            )
        }
    }

    @Test
    fun `Test task versionFile executes - custom file`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time = repo.commitTime(head)

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val task = project.tasks.getByName("versionFile") as VersionFileTask
            val file = project.layout.projectDirectory.file(".version").asFile
            task.file.set(file)
            task.actions.forEach { it.execute(task) }

            // then
            assertTrue(file.exists())
            assertEquals(
                """
                VERSION_BUILD=$headAbbreviated
                VERSION_BRANCH=main
                VERSION_BASE=
                VERSION_BRANCHID=main
                VERSION_BRANCHTYPE=main
                VERSION_COMMIT=$head
                VERSION_GRADLE=main-sha-$headAbbreviated
                VERSION_DISPLAY=main-sha-$headAbbreviated
                VERSION_FULL=main-sha-$headAbbreviated
                VERSION_SCM=git
                VERSION_TAG=
                VERSION_LAST_TAG=
                VERSION_DIRTY=false
                VERSION_VERSIONCODE=0
                VERSION_MAJOR=0
                VERSION_MINOR=0
                VERSION_PATCH=0
                VERSION_QUALIFIER=
                VERSION_TIME=$time
                """
                    .trimIndent(),
                file.readText().trim()
            )
        }
    }
}
