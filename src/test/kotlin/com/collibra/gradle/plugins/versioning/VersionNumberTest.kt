package com.collibra.gradle.plugins.versioning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VersionNumberTest {
    @Test
    fun `Test version code calculation`() {
        val versionNumber = VersionNumber(1, 2, 3, "qualifier", "1.2.3-qualifier")

        val expectedVersionCode = 1 * 10000 + 2 * 100 + 3
        assertEquals(expectedVersionCode, versionNumber.versionCode)
    }
}
