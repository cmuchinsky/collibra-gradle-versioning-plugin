package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.VersioningPlugin.Companion.DEFAULT_VERSION
import com.collibra.gradle.plugins.versioning.git.GitInfoService
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EdgeVersioningPluginTest :
    AbstractVersioningPluginTest(EdgeVersioningPlugin::class.java, mutableMapOf("baseVersion" to "0.1")) {

    companion object {
        private fun setupProject(baseVersion: String, branch: String, lastTag: String?, commit: String): Project {
            val project = ProjectBuilder.builder().build()
            project.extra.set("baseVersion", baseVersion)
            val scmInfo = buildSCMInfo(branch, lastTag, commit)
            every { GitInfoService.getInfo(any(), any()) } returns scmInfo
            every { GitInfoService.getLastTag(any(), any(), any()) } returns lastTag
            return project
        }
    }

    @BeforeEach
    fun beforeEach() {
        mockkObject(GitInfoService)
    }

    @AfterEach
    fun afterEach() {
        unmockkObject(GitInfoService)
    }

    /**
     * ******************************
     * RELEASE BRANCH *
     * ******************************
     */

    // Tests for patch increment, on release branch
    @Test
    fun `Patch based increment for release branch, increments the patch part of lastTag`() {
        // given
        val project = setupProject("notUsed", "release/1.0", "1.0.7", ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.0.8", project.version)
    }

    @Test
    fun `Patch based increment for release branch, and no displayVersion returned, use the default`() {
        // given
        // TODO versioningExtensionStub.setNoDisplayVersionReturned()
        val project = setupProject("notUsed", "release/1.0", "1.0.7", NUMERIC_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals(DEFAULT_VERSION, project.version)
    }

    @Test // I know this is kind of testing the stub, but I would like to have this test case
    // covered
    fun `Patch based increment for release branch, sets the patch part to 0 if no lastTag`() {
        // given
        val project = setupProject("notUsed", "release/1.0", null, ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.0.0", project.version)
    }

    /**
     * *******************************
     * MAIN BRANCH (patch increment) *
     * *******************************
     */

    // Tests for patch increment, on non-release branch, e.g. main
    @Test
    fun `Patch based increment for main branch, increments the patch part of lastTag`() {
        // given
        val project = setupProject("1.0", "main", "1.0.7", NUMERIC_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.0.8", project.version)
    }

    // Tests for patch increment, on non-release branch, e.g. main
    @Test
    fun `Patch based increment for main branch, and no displayVersion returned, use the default`() {
        // given
        // TODO versioningExtensionStub.setNoDisplayVersionReturned()
        val project = setupProject("1.0", "main", "1.0.7", ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals(DEFAULT_VERSION, project.version)
    }

    @Test // I know this is kind of testing the stub, but I would like to have this test case
    // covered
    fun `Patch based increment for main branch, sets the patch part to 0 if no lastTag`() {
        // given
        val project = setupProject("1.0", "main", null, NUMERIC_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.0.0", project.version)
    }

    /**
     * ******************************
     * MAIN BRANCH (minor increment) *
     * ******************************
     */

    // Tests for minor increment only, on non-release branch, e.g. main
    @Test
    fun `Minor based increment only for main branch, increments the minor part of lastTag`() {
        // given
        val project = setupProject("1", "main", "1.0.7", ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.1.0", project.version)
    }

    @Test
    fun `Minor based increment only for main branch, major increment if no lastTag`() {
        // given
        val project = setupProject("2", "main", null, NUMERIC_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("2.0.0", project.version)
    }

    @Test
    fun `Minor based increment only for main branch, set initial minor if no lastTag and baseVersion is 0`() {
        // given
        val project = setupProject("0", "main", null, ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("0.1.0", project.version)
    }

    /**
     * ******************************
     * NON AUTO-VERSIONING BRANCH *
     * ******************************
     */

    // Tests for patch increment, on non auto-versioning branch, e.g. my-feature
    @Test
    fun `non auto-versioning branch, and last tag found, set lastTag-build`() {
        // given
        val project = setupProject("1.0", "my-feature", "1.0.7", NUMERIC_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("1.0.7-${NUMERIC_COMMIT.take(7)}", project.version)
    }

    // Tests for patch increment, on non auto-versioning branch, e.g. my-feature
    @Test
    fun `non auto-versioning branch, and no last tag found, set default-build`() {
        // given
        // TODO versioningExtensionStub.setNoDisplayVersionReturned()
        val project = setupProject("1", "my-feature", "null", ALPHA_COMMIT)

        // when
        project.pluginManager.apply(plugin)

        // then
        assertEquals("$DEFAULT_VERSION-${ALPHA_COMMIT.take(7)}", project.version)
    }
}
