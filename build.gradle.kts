@file:Suppress("UnstableApiUsage")

import org.sonarqube.gradle.SonarTask

plugins {
    jacoco
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.versioning)
}

group = "com.collibra.gradle.plugins"

version = versioning.info.display

dependencies {
    implementation(gradleApi())
    implementation(libs.jgit)
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(libs.grgit.core)
                implementation(libs.mockk)
            }
            targets.all {
                testTask.configure {
                    environment("TEST_BRANCH", "feature/456-cute")
                    environment("BUILD_NUMBER", "12345")
                    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                }
            }
        }
    }
}

tasks.withType(JacocoReportBase::class) { mustRunAfter(tasks.withType(Test::class)) }

tasks.jacocoTestReport { reports { xml.required = true } }

tasks.withType(SonarTask::class) { dependsOn(tasks.jacocoTestReport) }

publishing {
    repositories {
        maven {
            name = "nexus"
            url = uri("https://nexus.collibra.com/repository/collibra-gradle-plugins")
            credentials(PasswordCredentials::class)
        }
    }
}

gradlePlugin {
    plugins {
        create("versioning") {
            id = "${project.group}.versioning"
            implementationClass = "com.collibra.gradle.plugins.versioning.VersioningPlugin"
        }
    }
}
