package com.collibra.gradle.plugins.versioning

import java.time.ZonedDateTime
import org.ajoberstar.grgit.Status

data class SCMInfo(
    val branch: String?,
    val commit: String?,
    val abbreviated: String?,
    val dateTime: ZonedDateTime?,
    val tag: String?,
    val lastTag: String?,
    val status: Status?,
    val dirty: Boolean,
    val shallow: Boolean
) {
    companion object {
        private val EMPTY: SCMInfo = SCMInfo(null, null, null, null, null, null, null, false, false)

        fun empty(): SCMInfo {
            return EMPTY
        }
    }

    fun isEmpty(): Boolean {
        return this == EMPTY
    }
}
