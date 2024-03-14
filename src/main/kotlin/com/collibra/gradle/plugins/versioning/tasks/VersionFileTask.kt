package com.collibra.gradle.plugins.versioning.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile

abstract class VersionFileTask : VersionDisplayTask() {
    @get:OutputFile abstract val file: RegularFileProperty

    init {
        description = "Writes version information into a file."
        prefix.convention("VERSION_")
        file.convention(project.layout.buildDirectory.file("version.properties"))
    }

    override fun run() {
        val outputFile = file.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(versionText(false))
    }
}
