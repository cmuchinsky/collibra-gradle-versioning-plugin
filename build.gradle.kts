@file:Suppress("UnstableApiUsage")

import org.sonarqube.gradle.SonarTask

plugins {
    groovy
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
    implementation(localGroovy())
    implementation(libs.grgit.core)
    implementation(libs.semver4j)
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies { implementation(libs.mockk) }
            targets.all {
                testTask.configure {
                    environment("GIT_TEST_BRANCH", "feature/456-cute")
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
        create("versioning-basic") {
            id = "${project.group}.versioning.basic"
            implementationClass = "com.collibra.gradle.plugins.versioning.plugins.BasicVersioningPlugin"
        }
        create("versioning-cloud") {
            id = "${project.group}.versioning.cloud"
            implementationClass = "com.collibra.gradle.plugins.versioning.plugins.CloudVersioningPlugin"
        }
        create("versioning-edge") {
            id = "${project.group}.versioning.edge"
            implementationClass = "com.collibra.gradle.plugins.versioning.plugins.EdgeVersioningPlugin"
        }
    }
}
