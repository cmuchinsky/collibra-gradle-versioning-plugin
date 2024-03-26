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

```kotlin
allprojects {
    version = versioning.info.display
}
```

## Applying the plug-in

```kotlin
plugins {
    id("com.collibra.gradle.plugins.versioning") version "x.x.x"
}
```

## Using the versioning info

For example, to set the project's _full_ version using the git version:

```kotlin
version = versioning.info.full
```

For a multi module project, you will probably do:

```kotlin
allprojects {
    version = versioning.info.full
}
```

## Versioning info

Once the `versioning` plug-in has been applied, a `versioning` extension is available for the project.

Getting the read-only `ìnfo` provides access to the following information, computed from the SCM information:

| Property                    | Description                                                              | Git: `main`                                | Git: `feature/great`                       | Git: `release/2.0`                         |
|-----------------------------|--------------------------------------------------------------------------|--------------------------------------------|--------------------------------------------|--------------------------------------------|
| `scm`                       | SCM source                                                               | `git`                                      | `git`                                      | `git`                                      |
| `branch`                    | Branch name                                                              | `main`                                     | `feature/great`                            | `release/2.0`                              |
| `branchType`                | Type of branch                                                           | `main`                                     | `feature`                                  | `release`                                  |
| `branchId`                  | Branch as an identifier                                                  | `main`                                     | `feature-great`                            | `release-2.0`                              |
| `commit`                    | Full commit hash                                                         | `09ef6297deb065f14704f9987301ee6620493f70` | `09ef6297deb065f14704f9987301ee6620493f70` | `09ef6297deb065f14704f9987301ee6620493f70` |
| `build`                     | Short commit/revision indicator, suitable for a build number             | `09ef629`                                  | `09ef629`                                  | `09ef629`                                  |
| `time`                      | Timestamp of the current commit                                          | (1)                                        | (1)                                        | (1)                                        |
| `tag`                       | Current tag                                                              | (2)                                        | (2)                                        | (2)                                        |
| `lastTag` (1)               | Last tag                                                                 | (3)                                        | (3)                                        | (3)                                        |
| `dirty`                     | Current state of the working copy                                        | (4)                                        | (4)                                        | (4)                                        |
| `shallow`                   | Shallow checkout                                                         | (5)                                        | (5)                                        | (5)                                        |
| `base`                      | Base version for the display version                                     | ''                                         | `great`                                    | `2.0`                                      |
| `full`                      | Branch ID and build                                                      | `main-09ef629`                             | `feature-great-09ef629`                    | `release-2.0-09ef629`                      |
| `display`                   | Display version                                                          | `main`                                     | `great`                                    | `2.0.0`, `2.0.1`, ...                      |
| `versionNumber`             | Version number containing major, minor, patch, qualifier and versionCode |                                            |                                            |                                            |
| `versionNumber.major`       | Major version                                                            | 0                                          | 0                                          | 2                                          |
| `versionNumber.minor`       | Minor version                                                            | 0                                          | 0                                          | 0                                          |
| `versionNumber.patch`       | Patch version                                                            | 0                                          | 0                                          | 0, 1, 2, ...                               |
| `versionNumber.qualifier`   | Version qualifier (alpha, beta, engineer, ...)                           | ''                                         | ''                                         | ''                                         |
| `versionNumber.versionCode` | Version code                                                             | 0                                          | 0                                          | 20000, 20001, ...                          |

* (1) Will be the timestamp of the current commit, or `null` if no timestamp is associated with it
* (2) Will be the name of the current tag if any, or `null` if no tag is associated to the current `HEAD`.
* (3) Name of the last tag on the branch. It can be on the current `HEAD` but not
necessarily - it will be `null` if no previous tag can be found. The last tags are
matched against the `lastTagPattern` regular expression defined in the configuration. It
defaults to `(\d+)$`, meaning that we just expect a sequence a digits at the end
of the tag name.
* (4) Depends on the state of the working copy the plug-in is applied to. `true` if the working copy contains uncommitted
  files.
* (5) Shallow checkout status. `true` if the working copy does not contain any parent commits

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
[version] scm         = git
[version] branch      = release/0.3
[version] branchType  = release
[version] branchId    = release-0.3
[version] commit      = da50c50567073d3d3a7756829926a9590f2644c6
[version] full        = release-0.3-da50c50
[version] base        = 0.3
[version] build       = da50c50
[version] gradle      = 0.3.0
[version] display     = 0.3.0
[version] tag         =
[version] lastTag     = 0.2.0
[version] dirty       = false
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

```kotlin
versionFile {
    // Path to the file to be written
    file = file(layout.buildDirectory.file("version.properties"))
    // Prefix to apply to the properties
    prefix = "VERSION_"
}
```

## Customization

The collection of the versioning info can be customised by setting some properties in the `versioning` extension.

The default properties are shown below:

```kotlin
versioning {
    // Fetch branch name from environment variables. Useful when using CI like Jenkins.
    branchEnv = listOf("GIT_BRANCH", "BRANCH_NAME")
}
```

### Dirty versions

The behaviour of the version computation is slightly different when the git repo is
dirty - meaning that the git repo contains some files which are not staged or not
committed. By default when the git repo is dirty, the `display` and `full` versions
are appended with the `-dirty` suffix.

This can be customised with the following attributes on the `versioning` extension:

```kotlin
versioning {
    // If set to `true`, the build will fail if the git repo is dirty and the branch is a release type.
    dirtyFailOnReleases = false
}
```

## Detached and shallow clone support

When a git repo is checked out in _detached_ mode, the `branch` will be set to `HEAD` and both the `display` and
`full` version will be set to `HEAD-<commit>` where `<commit>` is the abbreviated commit hash.

When a git repo is checked out in _shallow_ mode, no history is available and the `display` version for a _release_
branch cannot be correctly computed. In this case, we have two situations:

* if the `HEAD` commit has a tag, we use the tag name as `display` version
* if it has no tag, we use the `base` version and the SNAPSHOT suffix to indicate that the release's exact version
  cannot be computed.

In both cases, the `VersionInfo` object contains a `shallow` property which is set to `true`.

## External Git repository

In some very specific [cases](https://github.com/nemerosa/versioning/issues/37),
the Git directory might be external to the project.

In order to support this case, you can specify the `gitRepoRootDir` property:

```kotlin
versioning {
    gitRepoRootDir = "/path/to/other/directory"
}
```
