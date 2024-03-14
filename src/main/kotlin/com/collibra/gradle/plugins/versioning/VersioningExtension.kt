package com.collibra.gradle.plugins.versioning

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.api.Project
import org.semver4j.Semver

abstract class VersioningExtension(private val project: Project) {
    companion object {
        /** Registry of display modes */
        enum class DisplayModes {
            FULL,
            SNAPSHOT,
            BASE;

            open fun toDisplayVersion(
                branchType: String?,
                branchId: String?,
                branchBase: String,
                versionBuild: String?,
                versionFull: String?,
                extension: VersioningExtension
            ): String {
                return when (this) {
                    FULL -> "$branchId-$versionBuild"
                    SNAPSHOT -> "$branchBase${extension.snapshot}"
                    BASE -> branchBase
                }
            }
        }

        /** Registry of release modes */
        enum class ReleaseModes {
            TAG,
            SNAPSHOT;

            open fun toDisplayVersion(
                nextTag: String,
                lastTag: String,
                currentTag: String?,
                extension: VersioningExtension
            ): String {
                return when (this) {
                    TAG -> nextTag
                    SNAPSHOT ->
                        if (extension.releaseBuild && currentTag != null) currentTag
                        else "$nextTag${extension.snapshot}"
                }
            }
        }

        // Regex explained :
        // - 1st group one digit that is major version
        // - 2nd group one digit that is minor version
        // - It can be followed by a qualifier name
        // - 3rd group and last part is one digit that is patch version
        private val DISPLAY_REGEX = "(\\d+)[.](\\d+).*[.](\\d+)(.*)$".toRegex()

        private fun coerceVersion(version: String?): Semver {
            return Semver.coerce(version) ?: Semver(VersioningPlugin.DEFAULT_VERSION)
        }
    }

    /**
     * Allow setting the root git repo directory for non-conventional git/gradle setups. This is the path to the root
     * directory that contains the .git folder. This is used to validate if the current project is a git repository.
     */
    var gitRepoRootDir: String? by recomputeOnChange(null)

    /**
     * Fetch the branch from environment variable if available.
     *
     * By default, the environment is not taken into account, in order to be backward compatible with existing build
     * systems.
     */
    var branchEnv: List<String> by recomputeOnChange(ArrayList())

    /**
     * Getting the version type from a branch. Default: getting the part before the first "/" (or a second optional
     * 'separator' parameter). If no slash is found, takes the branch name as whole.
     *
     * For example:
     * * release/2.0 --> release
     * * feature/2.0 --> feature
     * * main --> release
     */
    var releaseParser: (SCMInfo, String?) -> ReleaseInfo by recomputeOnChange { scmInfo, separator ->
        ReleaseInfo.fromBranch(scmInfo.branch!!, separator)
    }

    /** Computes the full version. */
    var full: (SCMInfo) -> Any by recomputeOnChange { "${VersioningPlugin.normalize(it.branch)}-${it.abbreviated}" }

    /** Set of eligible branch types for computing a display version from the branch base name */
    var releases: Set<String> by recomputeOnChange(setOf(VersioningPlugin.RELEASE_BRANCH_TYPE))

    /** Display mode */
    var displayMode: Any by recomputeOnChange("full")

    /** Release mode */
    var releaseMode: Any by recomputeOnChange("tag")

    /** True if it's release build. Default is true, and branch should be in releases-set. */
    var releaseBuild: Boolean by recomputeOnChange(true)

    /** Default Snapshot extension */
    var snapshot: String by recomputeOnChange("-SNAPSHOT")

    /**
     * Dirty mode.
     *
     * Closure that takes a version (*display* or *full*) and processes it to produce a *dirty* indicator. By default,
     * it appends the [.dirtySuffix] value to the version.
     */
    var dirty: (Any) -> Any by recomputeOnChange { "$it$dirtySuffix" }

    /** Default dirty suffix */
    var dirtySuffix: String by recomputeOnChange("-dirty")

    /**
     * If set to `true`, the build will fail if working copy is dirty and if the branch type is part of the [.releases]
     * list ("release" only by default).
     */
    var dirtyFailOnReleases: Boolean by recomputeOnChange(false)

    /** If set to `true`, no warning will be printed in case the workspace is dirty. */
    var noWarningOnDirty: Boolean by recomputeOnChange(false)

    /** If set to `true`, displays the git status in case the workspace is dirty. */
    var dirtyStatusLog: Boolean by recomputeOnChange(false)

    /**
     * Pattern used to match when looking for the last tag. By default, checks for any tag having a last part being
     * numeric. At least one numeric grouping expression is required. The first one will be used to reverse order the
     * tags in Git.
     */
    var lastTagPattern: String by recomputeOnChange("(\\d+)$")

    /**
     * Digit precision for computing version code.
     *
     * With a precision of 2, 1.25.3 will become 12503. With a precision of 3, 1.25.3 will become 1250003.
     */
    var precision: Int by recomputeOnChange(2)

    /** Default number to use when no version number can be extracted from version string. */
    var defaultNumber: Int by recomputeOnChange(0)

    /** Closure that takes major, minor and patch integers in parameter and is computing versionCode number. */
    var computeVersionCode: (Int, Int, Int) -> Int by recomputeOnChange { major, minor, patch ->
        ((major * 10.toDouble().pow((2 * precision)).toInt()) + (minor * 10.toDouble().pow(precision).toInt()) + patch)
    }

    /**
     * Compute version number
     *
     * Closure that compute VersionNumber from *scmInfo*, *releaseType*, *branchId*, *versionFull*, *releaseBase* and
     * *versionDisplay*
     *
     * By default, it tries to find this pattern in display : '([0-9]+)[.]([0-9]+)[.]([0-9]+)(.*)$'. Version code is
     * computed with this algo : code = group(1) * 10^2precision + group(2) * 10^precision + group(3)
     *
     * Example :
     * - with precision = 2
     *
     * 1.2.3 -> 10203 10.55.62 -> 105562 20.3.2 -> 200302
     * - with precision = 3
     *
     * 1.2.3 -> 1002003 10.55.62 -> 100055062 20.3.2 -> 20003002
     */
    var parseVersionNumber:
        (SCMInfo?, String?, String?, String?, String?, String) -> VersionNumber by recomputeOnChange {
        _,
        _,
        _,
        _,
        _,
        versionDisplay ->
        // We are specifying all these parameters because we want to leave the choice to the
        // developer to use data that's right to them
        DISPLAY_REGEX.find(versionDisplay)?.let { result ->
            val major = result.groupValues[1].toInt()
            val minor = result.groupValues[2].toInt()
            val patch = result.groupValues[3].toInt()
            val qualifier = result.groupValues[4]
            VersionNumber(major, minor, patch, qualifier, computeVersionCode(major, minor, patch), versionDisplay)
        } ?: VersionNumber(0, 0, 0, "", defaultNumber, versionDisplay)
    }

    /**
     * Closure that computes a timestamp from a given ZonedDateTime.
     *
     * By default, an ISO-8601-compatible timestamp is computed.
     */
    var computeTimestamp: (ZonedDateTime?) -> String? by recomputeOnChange {
        it?.let { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) }
    }

    /** Closure that computes the semantic version from the *scmInfo*, *releaseInfo* and *versionDisplay*. */
    var computeSemantic: (SCMInfo, ReleaseInfo, String?) -> Semver by recomputeOnChange {
        scmInfo,
        releaseInfo,
        versionDisplay ->
        if (releaseInfo.branchType == VersioningPlugin.RELEASE_BRANCH_TYPE) {
            coerceVersion(versionDisplay)
        } else {
            coerceVersion(scmInfo.lastTag)
                .withPreRelease("${VersioningPlugin.PRE_RELEASE_PREFIX}${scmInfo.abbreviated}")
        }
    }

    private val computedInfo = AtomicReference(VersionInfo.empty())

    private fun <T> recomputeOnChange(initialValue: T): ReadWriteProperty<Any?, T> =
        object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) =
                computedInfo.set(VersionInfo.empty())
        }

    val info: VersionInfo
        get() = computedInfo.updateAndGet { if (it.isNotEmpty()) it else VersioningPlugin.computeInfo(project, this) }
}
