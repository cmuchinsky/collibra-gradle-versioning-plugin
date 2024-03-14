package com.collibra.gradle.plugins.versioning.tasks

import com.collibra.gradle.plugins.versioning.VersioningExtension
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VersionDisplayTask : DefaultTask() {
    @get:Input abstract val prefix: Property<String>

    init {
        group = "Versioning"
        description = "Writes version information on the standard output."
        prefix.convention("[version] ")
    }

    fun versionText(forDisplay: Boolean = true): String {
        val linePrefix = prefix.get()
        val info = project.extensions.getByType(VersioningExtension::class.java).info
        if (info.isEmpty() && forDisplay) {
            return "${linePrefix}No version can be computed from the SCM."
        }
        return mapOf(
                "build" to info.build,
                "branch" to info.branch,
                "base" to info.base,
                "branchId" to info.branchId,
                "branchType" to info.branchType,
                "commit" to info.commit,
                "gradle" to if (project.version == "unspecified") "" else project.version.toString(),
                "display" to info.display,
                "full" to info.full,
                "scm" to info.scm,
                "tag" to (info.tag ?: ""),
                "lastTag" to (info.lastTag ?: ""),
                "dirty" to info.dirty,
                "versionCode" to info.versionNumber!!.versionCode,
                "major" to info.versionNumber.major,
                "minor" to info.versionNumber.minor,
                "patch" to info.versionNumber.patch,
                "qualifier" to info.versionNumber.qualifier,
                "time" to (info.time ?: "")
            )
            .map { (key: String, value: Any?) ->
                if (forDisplay) {
                    "$linePrefix%-12s= $value".format(key)
                } else {
                    // Convert camelCase to snake_case for "lastTag" to preserve backwards
                    // compatibility
                    "$linePrefix${(if (key == "lastTag") "last_tag" else key).uppercase()}=$value"
                }
            }
            .joinToString(separator = "\n", postfix = "\n")
    }

    @TaskAction
    open fun run() {
        print(versionText())
    }
}
