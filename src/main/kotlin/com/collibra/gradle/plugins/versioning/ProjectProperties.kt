package com.collibra.gradle.plugins.versioning

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun readRequiredProperty(project: Project, property: String): String {
    if (!project.properties.containsKey(property)) {
        throw GradleException(
            "Could not find property $property in your project. Please add it to your gradle.properties file."
        )
    }
    return project.properties[property] as String
}

fun readOptionalProperty(project: Project, property: String): String {
    if (!project.properties.containsKey(property)) {
        project.extra[property] = ""
    }
    return project.properties[property] as String
}

fun readOptionalProperty(project: Project, property: String, defaultValue: String): String {
    return (project.properties[property] as String?).takeIf { !it.isNullOrBlank() } ?: defaultValue
}
