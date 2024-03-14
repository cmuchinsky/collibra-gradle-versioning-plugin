package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.VersioningPlugin.Companion.DEFAULT_VERSION
import com.collibra.gradle.plugins.versioning.VersioningPlugin.Companion.PRE_RELEASE_PREFIX
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CloudVersioningUnitTest : AbstractVersioningPluginTest(CloudVersioningPlugin::class.java) {

    @Test
    fun `Test auto versioning when branchType is 'release', use 'display'`() {
        // given
        val info = buildVersionInfo("release", "2.0.0", "1.9.0", ALPHA_COMMIT)

        // when
        val version = rawPluginInstance.buildVersion(info)

        // then
        assertEquals("2.0.0", version)
    }

    @Test
    fun `Test auto versioning when branchType is not 'release', use 'lastTag-commit'`() {
        // given
        val info = buildVersionInfo("feature", "2.0.0", "1.9.0", ALPHA_COMMIT)

        // when
        val version = rawPluginInstance.buildVersion(info)

        // then
        assertEquals("1.9.0-$PRE_RELEASE_PREFIX${ALPHA_COMMIT.take(7)}", version)
    }

    @Test
    fun `Test auto versioning is lenient when parsing non-semver input`() {
        // given
        val info = buildVersionInfo("feature", "2.0.0", "1.9", ALPHA_COMMIT)

        // when
        val version = rawPluginInstance.buildVersion(info)

        // then
        assertEquals("1.9.0-$PRE_RELEASE_PREFIX${ALPHA_COMMIT.take(7)}", version)
    }

    @Test
    fun `Test auto versioning works for numeric build info with leading zero`() {
        // given
        val featureInfo = buildVersionInfo("feature", "10.0.372", "10.0.372", NUMERIC_COMMIT)
        val releaseInfo = buildVersionInfo("release", "10.0.372", "10.0.372", NUMERIC_COMMIT)

        // when
        val featureVersion = rawPluginInstance.buildVersion(featureInfo)
        val releaseVersion = rawPluginInstance.buildVersion(releaseInfo)

        // then
        assertEquals("10.0.372-$PRE_RELEASE_PREFIX${NUMERIC_COMMIT.take(7)}", featureVersion)
        assertEquals("10.0.372", releaseVersion)
    }

    @Test
    fun `Test null versions`() {
        // given
        val featureInfo = buildVersionInfo("feature", null, null, ALPHA_COMMIT)
        val releaseInfo = buildVersionInfo("release", null, null, ALPHA_COMMIT)

        // when
        val featureVersion = rawPluginInstance.buildVersion(featureInfo)
        val releaseVersion = rawPluginInstance.buildVersion(releaseInfo)

        // then
        assertEquals("$DEFAULT_VERSION-$PRE_RELEASE_PREFIX${ALPHA_COMMIT.take(7)}", featureVersion)
        assertEquals(DEFAULT_VERSION, releaseVersion)
    }

    @Test
    fun `Test empty versions`() {
        // given
        val featureInfo = buildVersionInfo("feature", "", "", ALPHA_COMMIT)
        val releaseInfo = buildVersionInfo("release", "", "", ALPHA_COMMIT)

        // when
        val featureVersion = rawPluginInstance.buildVersion(featureInfo)
        val releaseVersion = rawPluginInstance.buildVersion(releaseInfo)

        // then
        assertEquals("$DEFAULT_VERSION-$PRE_RELEASE_PREFIX${ALPHA_COMMIT.take(7)}", featureVersion)
        assertEquals(DEFAULT_VERSION, releaseVersion)
    }
}
