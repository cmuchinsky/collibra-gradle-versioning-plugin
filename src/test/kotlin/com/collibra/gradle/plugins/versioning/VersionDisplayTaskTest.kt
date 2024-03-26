package com.collibra.gradle.plugins.versioning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class VersionDisplayTaskTest {
    @Test
    fun `Test task versionDisplay is present`() {
        // given
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(VersioningPlugin::class.java)

        // when
        val tasks = project.tasks.withType(VersionDisplayTask::class.java)

        // then
        assertNotNull(tasks)
        assertFalse(tasks.isEmpty())
    }

    @Test
    fun `Test task versionDisplay executes`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val task = project.tasks.getByName("versionDisplay")
            task.actions.forEach { it.execute(task) }
        }
    }
}
