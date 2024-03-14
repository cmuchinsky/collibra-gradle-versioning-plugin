package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.SCMInfo
import com.collibra.gradle.plugins.versioning.VersionInfo
import com.collibra.gradle.plugins.versioning.VersioningExtension
import com.collibra.gradle.plugins.versioning.VersioningPlugin
import java.time.ZonedDateTime
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

abstract class AbstractVersioningPluginTest(
    val plugin: Class<out Plugin<Project>>,
    private var extraProperties: MutableMap<String, Any> = mutableMapOf()
) {
    companion object {
        const val ALPHA_COMMIT = "abcdefghijklmnopqrstuvwxyzabcdefghiklmno"
        const val NUMERIC_COMMIT = "0123456789012345678901234567890123456789"

        fun buildSCMInfo(branch: String, lastTag: String?, commit: String): SCMInfo {
            return SCMInfo(branch, commit, commit.take(7), ZonedDateTime.now(), null, lastTag, null, false, false)
        }

        fun buildVersionInfo(branch: String, display: String?, lastTag: String?, commit: String): VersionInfo {
            val extension = object : VersioningExtension(ProjectBuilder.builder().build()) {}
            val scmInfo = buildSCMInfo(branch, lastTag, commit)
            val releaseInfo = extension.releaseParser(scmInfo, null)
            return VersionInfo(
                "git",
                scmInfo.branch!!,
                releaseInfo.branchType,
                "",
                commit,
                display,
                "",
                "",
                scmInfo.abbreviated!!,
                extension.computeTimestamp(scmInfo.dateTime),
                scmInfo.tag,
                scmInfo.lastTag,
                scmInfo.dirty,
                scmInfo.shallow,
                null,
                extension.computeSemantic(scmInfo, releaseInfo, display)
            )
        }
    }

    protected val rawPluginInstance = plugin.getDeclaredConstructor().newInstance() as VersioningPlugin

    @Test
    fun `When the plugin is applied then all tasks are present`() {
        val project: Project = projectWithPlugin(plugin)
        expectedTaskNames().forEach { taskName -> assertTaskIsPresent(project, taskName) }
        notExpectedTaskNames().forEach { taskName -> assertTasksIsNotPresent(project, taskName) }
    }

    private fun assertTaskIsPresent(project: Project, taskName: String) {
        val task = project.tasks.findByName(taskName)
        Assertions.assertNotNull(task, "The task '$taskName' is not present")
    }

    private fun assertTasksIsNotPresent(project: Project, taskName: String) {
        val task = project.tasks.findByName(taskName)
        Assertions.assertNull(task, "The task '$taskName' is present")
    }

    fun projectWithPlugin(plugin: Class<out Plugin<Project>>, props: Map<out String, Any> = emptyMap()): Project {
        val project: Project = ProjectBuilder.builder().build()
        extraProperties.putAll(props)
        extraProperties.entries.forEach { property -> project.extra.set(property.key, property.value) }
        project.pluginManager.apply(plugin)
        return project
    }

    protected open fun expectedTaskNames(): List<String> {
        return listOf("versionDisplay", "versionFile")
    }

    protected open fun notExpectedTaskNames(): List<String> {
        return emptyList()
    }
}
