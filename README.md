# collibra-gradle-versioning-plugin

This project provides the `com.collibra.gradle.plugins.versioning` gradle plugin, it was forked
from [nemerosa/versioning](https://github.com/nemerosa/versioning) version `3.1.0` and modified to
suit Collibra's needs (including the removal of SVN support and all of the svnkit transitive dependencies).

## Use cases

Given a simple release workflow:

![Release workflow](doc/release-workflow.png)

We get the version information from the branch in two flavours:

* the _full_ version, which is normalised branch name, followed by the short commit hash
* the _display_ version, which can be used to display the version to an end user, and is computed differently on a `feature/*` or `main` branch than on a `release/*` branch.

The computed project's _display_ version on the `feature/*` and `main` branches is the _base_ version (the normalised branch name without the prefix) and the abbreviated commit hash (or _build_ version). For `release/*` branches, the version is computed according the latest tag on the branch, allowing for automatic patch number.

To achieve such a configuration, just configure the `versioning` plug-in the following way and follow strict conventions for your branch names:

```groovy

allprojects {
   version = versioning.info.full
}

// Using versioning.info.display for generating property files for example
```

## Applying the plug-in

```groovy

plugins {
   id 'com.collibra.gradle.plugins.versioning' version 'x.x.x'
}
```

## Using the versioning info

For example, to set the project's _full_ version using the git version:

```groovy
version = versioning.info.full
```

For a multi module project, you will probably do:

```groovy
allprojects {
   version = versioning.info.full
}
```

## Versioning info

Once the `versioning` plug-in has been applied, a `versioning` extension is available for the project.

Getting the read-only `ìnfo` provides access to the following information, computed from the SCM information:

Property | Description | Git: `main`                                | Git: `feature/great`                       | Git: `release/2.0`
---|---|--------------------------------------------|--------------------------------------------|---
`scm` | SCM source | `git`                                      | `git`                                      | `git`
`branch` | Branch name | `main`                                     | `feature/great`                            | `release/2.0`
`branchType` | Type of branch | `main`                                     | `feature`                                  | `release`
`branchId` | Branch as an identifier | `main`                                     | `feature-great`                            | `release-2.0`
`commit` | Full commit hash | `09ef6297deb065f14704f9987301ee6620493f70` | `09ef6297deb065f14704f9987301ee6620493f70` | `09ef6297deb065f14704f9987301ee6620493f70`
`build` | Short commit/revision indicator, suitable for a build number | `09ef629`                                  | `09ef629`                                  | `09ef629`
`full` | Branch ID and build | `main-09ef629`                             | `feature-great-09ef629`                    | `release-2.0-09ef629`
`base` | Base version for the display version | ``                                         | `great`                                    | `2.0`
`gradle` | Project's version |                                            |                                            | 
`display` | Display version | `main`                                     | `great`                                    | `2.0.0`, `2.0.1`, ...
`tag` | Current tag | (1)                                        | (1)                                        | (1)
`lastTag` (1) | Last tag | (3)                                        | (3)                                        | (3)
`dirty` | Current state of the working copy | (2)                                        | (2)                                        | (2)
`versionNumber` | Version number containing major, minor, patch, qualifier and versionCode |                                            |                                            |  
`versionNumber.major` | Major version | 0                                          | 0                                          |  2
`versionNumber.minor` | Minor version | 0                                          | 0                                          |  0
`versionNumber.patch` | Patch version | 0                                          | 0                                          |  0, 1, 2, ...
`versionNumber.qualifier` | Version qualifier (alpha, beta, engineer, ...)| ''                                         | ''                                         | '' 
`versionNumber.versionCode` | Version code | 0                                          | 0                                          |  20000, 20001, 20002, ...
`time` | Timestamp of the current commit | (4)                                        | (4)                                        | (4)

* (1) will be the name of the current tag if any, or `null` if no tag is associated to the current `HEAD`.
* (2) depends on the state of the working copy the plug-in is applied to. `true` if the working copy contains uncommitted
files.
* (3) Name of the last tag on the branch. It can be on the current `HEAD` but not
necessarily - it will be `null` if no previous tag can be found. The last tags are
matched against the `lastTagPattern` regular expression defined in the configuration. It
defaults to `(\d+)$`, meaning that we just expect a sequence a digits at the end
of the tag name.
* (4) will be the timestamp of the current commit, or `null` if no timestamp is associated with it

### Display version

The `display` version is equal to the `base` property is available or to the branch identifier.

For branches to type `release`, an additional computation occurs:

* if no tag is available on the branch which has the `base` as a prefix, the `display` version is the `base` version, suffixed with `.0`
* if a tag is available on the branch which has the `base` as a prefix, the `display` version is this tag, where the last digit is incremented by 1

By using the `display` version when tagging a release, the `display` version will be automatically incremented, patch after patch, using the `release` base at a prefix.

### Version number

Version number is a container of several numbers computed from `display` by default . It is hosting major, minor, patch, 
qualifier and versionCode.

- In a tag like `1.2.3`, then major is `1`, minor is `2` and patch is `3`
- Qualifier are taken from tags formatted like `1.2-beta.0` where qualifier is `-beta` here
- Version code is a integer computed from major, minor and patch version.
    - `1.2.3` will give 10203
    - `21.5.16` will give 210516
    - `2.0-alpha.0` will give 20000

## Tasks

The `versioning` plug-in provides two tasks.

### `versionDisplay`

Displays the version information in the standard output. For example:

```bash
> ./gradlew versionDisplay
:versionDisplay
[version] scm        = git
[version] branch     = release/0.3
[version] branchType = release
[version] branchId   = release-0.3
[version] commit     = da50c50567073d3d3a7756829926a9590f2644c6
[version] full       = release-0.3-da50c50
[version] base       = 0.3
[version] build      = da50c50
[version] gradle     = 0.3.0
[version] display    = 0.3.0
[version] tag        =
[version] lastTag    = 0.2.0
[version] dirty      = false
[version] versionCode = 0
[version] major       = 0
[version] minor       = 0
[version] patch       = 0
[version] qualifier   = 
[version] time        = 2024-03-08T17:48:07+01:00
```

### `versionFile`

Creates a file which contains the version information. By default, the file is created at _build/version.properties_ and contains the following information:

```bash
> ./gradlew versionFile
> cat build/version.properties
VERSION_BUILD=da50c50
VERSION_BRANCH=release/0.3
VERSION_BASE=0.3
VERSION_BRANCHID=release-0.3
VERSION_BRANCHTYPE=release
VERSION_COMMIT=da50c50567073d3d3a7756829926a9590f2644c6
VERSION_GRADLE=0.3.0
VERSION_DISPLAY=0.3.0
VERSION_FULL=release-0.3-da50c50
VERSION_SCM=git
VERSION_TAG=
VERSION_LAST_TAG=0.2.0
VERSION_DIRTY=false
VERSION_VERSIONCODE=0
VERSION_MAJOR=0
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_QUALIFIER=
VERSION_TIME=2024-03-08T17:48:07+01:00
```

This makes this file easy to integrate in a Bash script:

```bash
export $(cat build/version.properties | xargs)
```

The `versionFile` task can be customised with two properties. The defaults are given below:

```groovy
versionFile {
   // Path to the file to be written
   file = new File(project.buildDir, 'version.properties')
   // Prefix to apply to the properties
   prefix = 'VERSION_'
}
```

## Customisation

The collection of the versioning info can be customised by setting some properties in the `versioning` extension.

The default properties are shown below:

```groovy
versioning {
   /**
    * Computation of the release type and the base, by parsing the scm info.
    * By default, we use "/" as a separator in branch name between the type and the base. If not
    * present, the type is the branch and the base is empty.
    * F.e. if you want use tag name instead of branch you may provide something like:
    */
    releaseParser = { gitInfo, separator = '/' ->
        List<String> part = gitInfo.tag.split('/') + ''
        new com.collibra.gradle.plugins.versioning.ReleaseInfo(part[0], part[1])
    }
    /**
     * Fetch branch name from environment variables. Useful when using CI like
     * Travis or Jenkins.
     */
    branchEnv = ['TRAVIS_BRANCH', 'GIT_BRANCH', 'SVN_BRANCH', 'BRANCH_NAME']
    /**
     * Computation of the full version
     */
    full = { gitInfo -> "${gitInfo.branch}-${gitInfo.abbreviated}" }
    /**
     * Set of eligible branch types for computing a display version from the branch base name
     */
    releases = ['release']
    /**
     * Pattern used to match when looking for the last tag. By default, checks for any
     * tag having a last part being numeric. At least one numeric grouping
     * expression is required. The first one will be used to reverse order
     * the tags in Git.
     */
    lastTagPattern = /(\d+)$/
}
```

### Dirty versions

The behaviour of the version computation is slightly different when the working
copy is dirty - meaning that the working copy contains some files which are not
staged or not committed.

When the working copy the version is computed from, the default behaviour is to
append the `-dirty` suffix to the `display` and `full` version.

This can be customised with the following attributes on the `versioning` extension:

```groovy
versioning {

   /**
   * Dirty mode.
   *
   * Closure that takes a version (display or full) and processes it to produce a <i>dirty</i>
   * indicator. By default, it appends the dirtySuffix value to the version.
   */
   dirty = { version -> "${version}${dirtySuffix}" }

   /**
   * Default dirty suffix
   */
   dirtySuffix = '-dirty'

   /**
   * If set to true, the build will fail if working copy is dirty and if the branch type is
   * part of the releases list ("release" only by default).
   */
   dirtyFailOnReleases = false

   /**
    * If set to true, no warning will be printed in case the workspace is dirty. Default is
    * to print a warning.
    */
   noWarningOnDirty = false

  /**
   * If set to true, displays the scm status in case the workspace is dirty. Default is false.
   */
  dirtyStatusLog = false
}
```

### Snapshots

Recommended configuration for development with `-SNAPSHOT`, for details see below.

```groovy
versioning {
   releaseBuild = false		// own control, which build is really release, set to true in CI-server release job
   releaseMode = 'snapshot'	// how to compute version on release-branches
   displayMode = 'snapshot'	// how to compute version on non-release-branches
   dirty = { t -> t }		// switch off dirty-suffix ( could be usefull for local development )
}
```

#### Snapshots on release branches

Sometimes, you do not want to have the `display` version for a _release_ branch being the next tag if you are already on a tag.
( _release_ branch is defined in the `releases`-set. )

By default, the `versioning` plug-in will behave correctly if you tag only as the very end of your delivery pipeline,
when the project is actually delivered. But if you want to tag upfront, you probably need to indicate that your `display` version
is a _snapshot_ or similar (see issue [#19|https://github.com/nemerosa/versioning/issues/19] for the discussion).

In such a case, you can specify a `snapshot` release mode:

```groovy
versioning {
   releaseMode = 'snapshot'
}
```

In this case, if the `HEAD` is not _exactly_ associated with a tag, the `-SNAPSHOT` string will be appended to the `display` version. For example, if there is a previous tag `2.1.0`, then the `display` version will be `2.1.1-SNAPSHOT`. But if the `HEAD` is exactly on the `2.1.0` tag, then the `display` version is also `2.1.0`.

You can customise the `-SNAPSHOT` suffix used the `snapshot` property:

```groovy
versioning {
   releaseMode = 'snapshot'
   snapshot = '.DEV'
}
```

Note that the default `releaseMode` is `tag`, where the next tag is always used as a `display` version.

You can also customise completely the computation of the `display` version for a release by setting the `releaseMode` to a `Closure`:

```groovy
versioning {
   releaseMode = { nextTag, lastTag, currentTag, extension ->
       "${nextTag}"
   }
}
```

The meaning of the parameters is illustrated by the diagrams below:

![Release tags](doc/release-tags.png)

* `nextTag` = `2.1.2` - computed
* `lastTag` = `2.1.1` - last tag from the `HEAD`
* `currentTag` = `2.1.1` - exact tag for the `HEAD`

![Release tags](doc/release-tags-none.png)

* `nextTag` = `2.1.1` - computed
* `lastTag` = `2.1.0` - last tag from the `HEAD`
* `currentTag` = none - exact tag for the `HEAD`

The `extension` parameter is the content of the `versioning` configuration object.

**Note** that the display mode based on the current tag is **not supported in Subversion**. It is kind of tricky
to get the tag associated to a given revision.

If You want to have more control when release or snapshot build is performed, You can use `releaseBuild` boolean property.
This is useful in case of usage central maven repository, which forbids replacement of released final artifacts.

```groovy
versioning {
   releaseBuild = false // this could be set as build argument of gradle
   releaseMode = 'snapshot'
}
```

#### Snapshots on non-release branches

Non-release branches are all which doesn't exists in `release`-set.
In case you prefer maven way for creating artifacts and You want to use `-SNAPSHOT` suffix , it's recomended to use `displayMode`.

```groovy
versioning {
   displayMode = 'snapshot'
}
```

Possible values for `displayMode` are defined as closures :
```groovy
DISPLAY_MODES = [
    full    : { branchType, branchId, base, build, full, extension ->
                "${branchId}-${build}"
              },
    snapshot: { branchType, branchId, base, build, full, extension ->
                "${base}${extension.snapshot}"
              },
    base    : { branchType, branchId, base, build, full, extension ->
                base
              },
]
```

You can use also use own display mode defined as a closure :
```groovy
versioning {
   displayMode = { branchType, branchId, base, build, full, extension ->
                "${branchType}-${base}${extension.snapshot}"
            }
}
```
### Version number

Version number computation can be customised by setting some properties in the `versioning` extension.

```groovy
versioning {
    /**
     * Digit precision for computing version code.
     *
     * With a precision of 2, 1.25.3 will become 12503.
     * With a precision of 3, 1.25.3 will become 1250003.
     */
    int precision = 2

    /**
     * Default number to use when no version number can be extracted from version string.
     */
    int defaultNumber = 0

    /**
     * Closure that takes major, minor and patch integers in parameter and is computing versionCode number.
     */
    Closure<Integer> computeVersionCode = { int major, int minor, int patch ->
        return (major * 10**(2 * precision)) + (minor * 10**precision) + patch
    }

    /**
     * Compute version number
     *
     * Closure that compute VersionNumber from <i>gitInfo</i>, <i>versionReleaseType</i>, <i>versionBranchId</i>,
     * <i>versionFull</i>, <i>versionBase</i> and <i>versionDisplay</i>
     *
     * By default it tries to find this pattern in display : '([0-9]+)[.]([0-9]+)[.]([0-9]+)(.*)$'.
     * Version code is computed with this algo : code = group(1) * 10^2precision + group(2) * 10^precision + group(3)
     *
     * Example :
     *
     * - with precision = 2
     *
     * 1.2.3 -> 10203
     * 10.55.62 -> 105562
     * 20.3.2 -> 200302
     *
     * - with precision = 3
     *
     * 1.2.3 -> 1002003
     * 10.55.62 -> 100055062
     * 20.3.2 -> 20003002
     **/
    Closure<VersionNumber> parseVersionNumber = { SCMInfo gitInfo, String versionReleaseType, String versionBranchId,
                                                  String versionFull, String versionBase, String versionDisplay ->
        // We are specifying all these parameters because we want to leave the choice to the developer
        // to use data that's right to him
        // Regex explained :
        // - 1st group one digit that is major version
        // - 2nd group one digit that is minor version
        // - It can be followed by a qualifier name
        // - 3rd group and last part is one digit that is patch version
        Matcher m = (versionDisplay =~ '([0-9]+)[.]([0-9]+).*[.]([0-9]+)(.*)$')
        if (m.find()) {
            try {
                int n1 = Integer.parseInt(m.group(1))
                int n2 = Integer.parseInt(m.group(2))
                int n3 = Integer.parseInt(m.group(3))
                String q = m.group(4) ?: ''
                return new VersionNumber(n1, n2, n3, q, computeVersionCode(n1, n2, n3).intValue(), versionDisplay)
            } catch (Exception ignore) {
                // Should never go here
                return new VersionNumber(0, 0, 0, '', defaultNumber, versionDisplay)
            }
        } else {
            return new VersionNumber(0, 0, 0, '', defaultNumber, versionDisplay)
        }
    }

}
```

### Timestamp

The timestamp computation can be customised in the `versioning` extension.

By default, an [ISO-8601-compatible](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_OFFSET_DATE_TIME) timestamp is computed.

[SOURCE_DATE_EPOCH](https://reproducible-builds.org/docs/source-date-epoch/)-compatible:
```groovy
import java.time.ZonedDateTime

versioning {

    Closure<String> computeTimestamp = { ZonedDateTime t ->
        t == null ? null : Long.toString(t.toEpochSecond())
    }
}
```
RFC-1123-compatible using [DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html):

```groovy
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

versioning {

    Closure<String> computeTimestamp = { ZonedDateTime t ->
        t == null ? null : DateTimeFormatter.RFC_1123_DATE_TIME.format(t)
    }
}
```

## Detached and shallow clone support

When a repository is checked out in _detached_ mode, the `branch` will be set to `HEAD` and both the `display` and
`full` version will be set to `HEAD-<commit>` where `<commit>` is the abbreviated commit hash.

When a repository is checked out in _shallow_ mode, no history is available and the `display` version for a _release_
branch cannot be correctly computed. In this case, we have two situations:

* if the `HEAD` commit has a tag, we use the tag name as `display` version
* if it has no tag, we use the `base` version and the SNAPSHOT suffix to indicate that the release's exact version
  cannot be computed.

In both cases, the `VersionInfo` object contains a `shallow` property which is set to `true`.

## External Git repository

In some very specific [cases](https://github.com/nemerosa/versioning/issues/37),
the Git directory might be external to the project.

In order to support this case, you can specify the `gitRepoRootDir` property:

```groovy
versioning {
    gitRepoRootDir = '/path/to/other/directory'
}
```
