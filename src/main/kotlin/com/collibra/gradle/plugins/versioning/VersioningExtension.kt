package com.collibra.gradle.plugins.versioning

import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.api.Project

abstract class VersioningExtension(private val project: Project) {
    /**
     * Allow setting the root git repo directory for non-conventional git/gradle setups. This is the path to the root
     * directory that contains the .git folder. This is used to validate if the current project is a git repository.
     */
    var gitRepoRootDir: String? by recomputeOnChange(null)

    /**
     * Fetch the branch from environment variable if available. By default, the environment is not taken into account,
     * in order to be backward compatible with existing build systems.
     */
    var branchEnv: List<String> by
        recomputeOnChange(
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    computedInfo.set(VersionInfo.empty())
                    return super.add(element)
                }

                override fun clear() {
                    computedInfo.set(VersionInfo.empty())
                    super.clear()
                }

                override fun remove(element: String): Boolean {
                    computedInfo.set(VersionInfo.empty())
                    return super.remove(element)
                }
            }
        )

    /** If set to `true`, the build will fail if the git repo is dirty and the branch is a release type. */
    var dirtyFailOnReleases: Boolean by recomputeOnChange(false)

    /**
     * If set to `true`, the versioning behavior will mimic that of the Collibra jenkins-pipeline-general's
     * `autoVersionFromFile` function, which derives its version from the Gradle `version` plus the Jenkins build
     * number.
     */
    var jenkinsMode: Boolean by recomputeOnChange(false)

    private val computedInfo = AtomicReference(VersionInfo.empty())

    private fun <T> recomputeOnChange(initialValue: T): ReadWriteProperty<Any?, T> =
        object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) =
                computedInfo.set(VersionInfo.empty())
        }

    val info: VersionInfo
        get() = computedInfo.updateAndGet { if (it.isNotEmpty()) it else VersioningPlugin.computeInfo(project, this) }
}
