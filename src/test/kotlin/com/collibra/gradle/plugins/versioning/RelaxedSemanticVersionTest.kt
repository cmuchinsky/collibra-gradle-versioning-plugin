package com.collibra.gradle.plugins.versioning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RelaxedSemanticVersionTest {
    @Test
    fun `Test version null`() {
        val version = RelaxedSemanticVersion(null)
        assertEquals(0, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("", version.relaxedVersion())
        assertEquals("0.0.0", version.strictVersion())
    }

    @Test
    fun `Test version empty`() {
        val version = RelaxedSemanticVersion("")
        assertEquals(0, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("", version.relaxedVersion())
        assertEquals("0.0.0", version.strictVersion())
    }

    @Test
    fun `Test version blank`() {
        val version = RelaxedSemanticVersion(" \t")
        assertEquals(0, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("", version.relaxedVersion())
        assertEquals("0.0.0", version.strictVersion())
    }

    @Test
    fun `Test version non-numeric`() {
        val version = RelaxedSemanticVersion("foo")
        assertEquals(0, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("", version.relaxedVersion())
        assertEquals("0.0.0", version.strictVersion())
    }

    @Test
    fun `Test version major`() {
        val version = RelaxedSemanticVersion("1")
        assertEquals(1, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertEquals(listOf("1.0.0", "1.0", "1"), version.equivalents())
        assertEquals("1", version.relaxedVersion())
        assertEquals("1.0.0", version.strictVersion())
    }

    @Test
    fun `Test version major and preRelease`() {
        val version = RelaxedSemanticVersion("1-alpha")
        assertEquals(1, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("alpha", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-alpha", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1-alpha", version.relaxedVersion())
        assertEquals("1.0.0-alpha", version.strictVersion())
    }

    @Test
    fun `Test version major and preRelease with custom preRelease`() {
        val version = RelaxedSemanticVersion("1-alpha").withQualifier("beta")
        assertEquals(1, version.major())
        assertEquals(0, version.minor())
        assertEquals(0, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("beta", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-beta", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1-beta", version.relaxedVersion())
        assertEquals("1.0.0-beta", version.strictVersion())
    }

    @Test
    fun `Test version major and minor`() {
        val version = RelaxedSemanticVersion("1.2")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertEquals(listOf("1.2.0", "1.2"), version.equivalents())
        assertEquals("1.2", version.relaxedVersion())
        assertEquals("1.2.0", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, and preRelease`() {
        val version = RelaxedSemanticVersion("1.2-alpha")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(0, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("alpha", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-alpha", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2-alpha", version.relaxedVersion())
        assertEquals("1.2.0-alpha", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, and preRelease with custom preRelease`() {
        val version = RelaxedSemanticVersion("1.2-alpha").withQualifier("beta")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(0, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("beta", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-beta", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2-beta", version.relaxedVersion())
        assertEquals("1.2.0-beta", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, and patch`() {
        val version = RelaxedSemanticVersion("1.2.3")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(3, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2.3", version.relaxedVersion())
        assertEquals("1.2.3", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, patch, and preRelease`() {
        val version = RelaxedSemanticVersion("1.2.3-alpha")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(3, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("alpha", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-alpha", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2.3-alpha", version.relaxedVersion())
        assertEquals("1.2.3-alpha", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, patch, and preRelease with custom preRelease`() {
        val version = RelaxedSemanticVersion("1.2.3-alpha").withQualifier("beta")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(3, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("beta", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-beta", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2.3-beta", version.relaxedVersion())
        assertEquals("1.2.3-beta", version.strictVersion())
    }

    @Test
    fun `Test version major, minor, patch, preRelease, and build`() {
        val version = RelaxedSemanticVersion("1.2.3-alpha+build")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(3, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("alpha", preRelease[0])
        val build = version.build()
        assertTrue(build.isNotEmpty())
        assertEquals(1, build.size)
        assertEquals("build", build[0])
        assertEquals("-alpha+build", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2.3-alpha+build", version.relaxedVersion())
        assertEquals("1.2.3-alpha+build", version.strictVersion())
    }

    @Test
    fun `Test version version major, minor, patch, preRelease, and build with custom preRelease`() {
        val version = RelaxedSemanticVersion("1.2.3-alpha+build").withQualifier("beta")
        assertEquals(1, version.major())
        assertEquals(2, version.minor())
        assertEquals(3, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("beta", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-beta", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("1.2.3-beta", version.relaxedVersion())
        assertEquals("1.2.3-beta", version.strictVersion())
    }

    @Test
    fun `Test version year and month`() {
        val version = RelaxedSemanticVersion("2024.10")
        assertEquals(2024, version.major())
        assertEquals(10, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertEquals(listOf("2024.10.0", "2024.10"), version.equivalents())
        assertEquals("2024.10", version.relaxedVersion())
        assertEquals("2024.10.0", version.strictVersion())
    }

    @Test
    fun `Test version year and padded month`() {
        val version = RelaxedSemanticVersion("2024.05")
        assertEquals(2024, version.major())
        assertEquals(5, version.minor())
        assertEquals(0, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertEquals(listOf("2024.5.0", "2024.5"), version.equivalents())
        assertEquals("2024.05", version.relaxedVersion())
        assertEquals("2024.5.0", version.strictVersion())
    }

    @Test
    fun `Test version year, month, and patch`() {
        val version = RelaxedSemanticVersion("2024.10.1")
        assertEquals(2024, version.major())
        assertEquals(10, version.minor())
        assertEquals(1, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("2024.10.1", version.relaxedVersion())
        assertEquals("2024.10.1", version.strictVersion())
    }

    @Test
    fun `Test version year, padded month, and patch`() {
        val version = RelaxedSemanticVersion("2024.05.1")
        assertEquals(2024, version.major())
        assertEquals(5, version.minor())
        assertEquals(1, version.patch())
        assertTrue(version.preRelease().isEmpty())
        assertTrue(version.build().isEmpty())
        assertTrue(version.qualifier().isEmpty())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("2024.05.1", version.relaxedVersion())
        assertEquals("2024.5.1", version.strictVersion())
    }

    @Test
    fun `Test version year, padded month, and single-segment preRelease`() {
        val version = RelaxedSemanticVersion("2024.05.0-main-1000")
        assertEquals(2024, version.major())
        assertEquals(5, version.minor())
        assertEquals(0, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(1, preRelease.size)
        assertEquals("main-1000", preRelease[0])
        assertTrue(version.build().isEmpty())
        assertEquals("-main-1000", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("2024.05.0-main-1000", version.relaxedVersion())
        assertEquals("2024.5.0-main-1000", version.strictVersion())
    }

    @Test
    fun `Test version year, padded month, and multi-segment preRelease`() {
        val version = RelaxedSemanticVersion("2024.05.1-main.10000")
        assertEquals(2024, version.major())
        assertEquals(5, version.minor())
        assertEquals(1, version.patch())
        val preRelease = version.preRelease()
        assertTrue(preRelease.isNotEmpty())
        assertEquals(2, preRelease.size)
        assertEquals("main", preRelease[0])
        assertEquals("10000", preRelease[1])
        assertTrue(version.build().isEmpty())
        assertEquals("-main.10000", version.qualifier())
        assertTrue(version.equivalents().isEmpty())
        assertEquals("2024.05.1-main.10000", version.relaxedVersion())
        assertEquals("2024.5.1-main.10000", version.strictVersion())
    }
}
