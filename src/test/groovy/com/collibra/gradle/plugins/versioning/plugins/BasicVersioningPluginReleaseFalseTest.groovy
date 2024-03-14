package com.collibra.gradle.plugins.versioning.plugins

import com.collibra.gradle.plugins.versioning.ReleaseInfo
import com.collibra.gradle.plugins.versioning.SCMInfo
import com.collibra.gradle.plugins.versioning.VersionInfo
import com.collibra.gradle.plugins.versioning.git.GitRepo
import com.collibra.gradle.plugins.versioning.support.DirtyException
import com.collibra.gradle.plugins.versioning.tasks.VersionDisplayTask
import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.assertThrows

class BasicVersioningPluginReleaseFalseTest {
    @Test
    void 'Git not present'() {
        def wd = File.createTempDir('git', '')
        def project = ProjectBuilder.builder().withProjectDir(wd).build()
        new BasicVersioningPlugin().apply(project)
        project.versioning {
            releaseBuild = false
        }
        VersionInfo info = project.versioning.info as VersionInfo
        assert info != null
        assert info.isEmpty()
    }

    @Test
    void 'Git master'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'main'
            assert info.base == ''
            assert info.branchId == 'main'
            assert info.branchType == 'main'
            assert info.commit == head
            assert info.display == "main-${headAbbreviated}" as String
            assert info.full == "main-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    /**
     * When a branch is checked out using a detached HEAD, the branch type will be set to
     * `detached`.
     */
    @Test
    void 'Git detached HEAD'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def commit3 = repo.commitLookup('Commit 3')
            def commit3Abbreviated = repo.commitLookup('Commit 3', true)
            def time = getCommitTime(repo, commit3)

            // Creates a temporary directory where to perform a detached clone operation
            File detached = File.createTempDir('git', '')
            try {

                // Cloning
                def git = Git.cloneRepository()
                        .setURI(repo.dir.toURI().toString())
                        .setDirectory(detached)
                        .call()
                // Detached HEAD
                git.checkout().setName(commit3).call()

                def project = ProjectBuilder.builder().withProjectDir(detached).build()
                new BasicVersioningPlugin().apply(project)
                project.versioning {
                    releaseBuild = false
                }
                VersionInfo info = project.versioning.info as VersionInfo
                assert info != null
                assert info.build == commit3Abbreviated
                assert info.branch == 'HEAD'
                assert info.base == ''
                assert info.branchId == 'HEAD'
                assert info.branchType == 'HEAD'
                assert info.commit == commit3
                assert info.display == "HEAD-${commit3Abbreviated}" as String
                assert info.full == "HEAD-${commit3Abbreviated}" as String
                assert info.scm == 'git'
                assert info.tag == null
                assert !info.dirty
                assert info.versionNumber.versionCode == 0
                assert info.time == time
            } finally {
                detached.deleteDir()
            }
        }
    }

    /**
     * The Git information is accessible from a sub project.
     * @issue # 20
     */
    @Test
    void 'Git sub project'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            def subdir = new File(repo.dir, 'sub')
            subdir.mkdirs()
            def subproject = ProjectBuilder.builder().withParent(project).withProjectDir(subdir).build()
            new BasicVersioningPlugin().apply(subproject)
            subproject.versioning {
                releaseBuild = false
            }
            VersionInfo info = subproject.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'main'
            assert info.base == ''
            assert info.branchId == 'main'
            assert info.branchType == 'main'
            assert info.commit == head
            assert info.display == "main-${headAbbreviated}" as String
            assert info.full == "main-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git shallow history for master'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            // Creates a temporary directory where to perform a shallow clone operation
            File detached = File.createTempDir('git', '')
            try {

                new ProcessBuilder('git', 'clone', '--depth', '1', "file://${repo.dir.absolutePath}", '.')
                        .directory(detached)
                        .start()
                        .waitForOrKill(2000L)

                def project = ProjectBuilder.builder().withProjectDir(detached).build()
                new BasicVersioningPlugin().apply(project)
                project.versioning {
                    releaseBuild = false
                }
                VersionInfo info = project.versioning.info as VersionInfo
                assert info != null
                assert info.build == headAbbreviated
                assert info.branch == 'main'
                assert info.base == ''
                assert info.branchId == 'main'
                assert info.branchType == 'main'
                assert info.commit == head
                assert info.display == "main-${headAbbreviated}" as String
                assert info.full == "main-${headAbbreviated}" as String
                assert info.scm == 'git'
                assert info.tag == null
                assert !info.dirty
                assert info.shallow
                assert info.versionNumber.versionCode == 0
                assert info.time == time
            } finally {
                detached.deleteDir()
            }
        }
    }

    @Test
    void 'Git display version'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            def task = project.tasks.getByName('versionDisplay') as VersionDisplayTask
            task.actions.each { action ->
                action.execute(task)
            }
        }
    }

    @Test
    void 'Git version file - defaults'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            def task = project.tasks.getByName('versionFile') as DefaultTask
            task.actions.each { action ->
                action.execute(task)
            }

            // Checks the file
            def file = project.layout.buildDirectory.file('version.properties').get().asFile
            assert file.exists(): "File ${file} must exist."
            assert file.text == """\
VERSION_BUILD=${headAbbreviated}
VERSION_BRANCH=main
VERSION_BASE=\n\
VERSION_BRANCHID=main
VERSION_BRANCHTYPE=main
VERSION_COMMIT=${head}
VERSION_GRADLE=main-${headAbbreviated}
VERSION_DISPLAY=main-${headAbbreviated}
VERSION_FULL=main-${headAbbreviated}
VERSION_SCM=git
VERSION_TAG=
VERSION_LAST_TAG=
VERSION_DIRTY=false
VERSION_VERSIONCODE=0
VERSION_MAJOR=0
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_QUALIFIER=
VERSION_TIME=${time}
""" as String
        }
    }

    @Test
    void 'Git version file - custom prefix'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            project.versionFile {
                prefix = 'CUSTOM_'
            }
            def task = project.tasks.getByName('versionFile') as DefaultTask
            task.actions.each { action ->
                action.execute(task)
            }

            // Checks the file
            def file = project.layout.buildDirectory.file('version.properties').get().asFile
            assert file.exists(): "File ${file} must exist."
            assert file.text == """\
CUSTOM_BUILD=${headAbbreviated}
CUSTOM_BRANCH=main
CUSTOM_BASE=\n\
CUSTOM_BRANCHID=main
CUSTOM_BRANCHTYPE=main
CUSTOM_COMMIT=${head}
CUSTOM_GRADLE=main-${headAbbreviated}
CUSTOM_DISPLAY=main-${headAbbreviated}
CUSTOM_FULL=main-${headAbbreviated}
CUSTOM_SCM=git
CUSTOM_TAG=
CUSTOM_LAST_TAG=
CUSTOM_DIRTY=false
CUSTOM_VERSIONCODE=0
CUSTOM_MAJOR=0
CUSTOM_MINOR=0
CUSTOM_PATCH=0
CUSTOM_QUALIFIER=
CUSTOM_TIME=${time}
""" as String
        }
    }

    @Test
    void 'Git version file - custom file'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
            }
            def head = repo.commitLookup('Commit 4')
            def headAbbreviated = repo.commitLookup('Commit 4', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            project.versionFile {
                file = new File(repo.dir, '.version')
            }
            def task = project.tasks.getByName('versionFile') as DefaultTask
            task.actions.each { action ->
                action.execute(task)
            }

            // Checks the file
            def file = new File(project.projectDir, '.version')
            assert file.exists(): "File ${file} must exist."
            assert file.text == """\
VERSION_BUILD=${headAbbreviated}
VERSION_BRANCH=main
VERSION_BASE=\n\
VERSION_BRANCHID=main
VERSION_BRANCHTYPE=main
VERSION_COMMIT=${head}
VERSION_GRADLE=main-${headAbbreviated}
VERSION_DISPLAY=main-${headAbbreviated}
VERSION_FULL=main-${headAbbreviated}
VERSION_SCM=git
VERSION_TAG=
VERSION_LAST_TAG=
VERSION_DIRTY=false
VERSION_VERSIONCODE=0
VERSION_MAJOR=0
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_QUALIFIER=
VERSION_TIME=${time}
""" as String
        }
    }

    @Test
    void 'Git feature branch'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}" as String
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch with full display mode'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                displayMode = 'full'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}" as String
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch with snapshot mode'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                displayMode = 'snapshot'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "123-great-SNAPSHOT"
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch with custom snapshot mode'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                displayMode = 'snapshot'
                snapshot = '.DEV'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "123-great.DEV"
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch with base mode'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                displayMode = 'base'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "123-great"
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch with custom mode'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                displayMode = { branchType, branchId, base, build, full, extension ->
                    "${base}-${build}-SNAPSHOT"
                }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "123-great-${headAbbreviated}-SNAPSHOT" as String
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: no previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.0'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20000
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tag alpha'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0-alpha'
                commit 5
                tag '2.0-alpha.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0-alpha'
            assert info.base == '2.0-alpha'
            assert info.branchId == 'release-2.0-alpha'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0-alpha.3'
            assert info.full == "release-2.0-alpha-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tags alpha, older tag must be taken into account'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/3.0-alpha'
                commit 5
                tag '3.0-alpha.0'
                sleep 1000
                commit 6
                tag '3.0-alpha.1'
                commit 7
            }
            def head = repo.commitLookup('Commit 7')
            def headAbbreviated = repo.commitLookup('Commit 7', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo

            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/3.0-alpha'
            assert info.base == '3.0-alpha'
            assert info.branchId == 'release-3.0-alpha'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '3.0-alpha.2'
            assert info.full == "release-3.0-alpha-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 30002
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tags alpha, chronological order of tags must be taken into account'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/3.0-alpha'
                commit 5
                tag '3.0-alpha.9'
                sleep 1000
                commit 6
                tag '3.0-alpha.10'
                commit 7
            }
            def head = repo.commitLookup('Commit 7')
            def headAbbreviated = repo.commitLookup('Commit 7', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo

            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/3.0-alpha'
            assert info.base == '3.0-alpha'
            assert info.branchId == 'release-3.0-alpha'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '3.0-alpha.11'
            assert info.full == "release-3.0-alpha-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 30011
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tags alpha, chronological order of tags must be taken into account - 2'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/3.0-alpha'
                commit 5
                tag '3.0-alpha.19'
                sleep 1000
                commit 6
                tag '3.0-alpha.20'
                commit 7
            }
            def head = repo.commitLookup('Commit 7')
            def headAbbreviated = repo.commitLookup('Commit 7', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo

            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/3.0-alpha'
            assert info.base == '3.0-alpha'
            assert info.branchId == 'release-3.0-alpha'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '3.0-alpha.21'
            assert info.full == "release-3.0-alpha-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 30021
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tag on different branches'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                branch 'release/2.1'
                commit 6
                tag '2.1.0'
                checkout 'release/2.0'
                commit 7
            }
            def head = repo.commitLookup('Commit 7')
            def headAbbreviated = repo.commitLookup('Commit 7', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch: with previous tag with two final digits'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.10'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.11'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20011
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with snapshot: no previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
            }

            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.0-SNAPSHOT'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20000
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with custom snapshot: no previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
                snapshot = '-DEV'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.0-DEV'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20000
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with custom display: no previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = { nextTag, lastTag, currentTag, extension -> "${nextTag}-PREVIEW" }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.0-PREVIEW'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20000
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with snapshot: with previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-SNAPSHOT'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with custom snapshot: with previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
                snapshot = '-DEV'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-DEV'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time

        }
    }

    @Test
    void 'Git release branch with custom display: with previous tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = { nextTag, lastTag, currentTag, extension -> "${nextTag}-PREVIEW" }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-PREVIEW'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with snapshot: on tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                commit 6
                tag '2.0.2'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-SNAPSHOT'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == '2.0.2'
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with custom snapshot: on tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                commit 6
                tag '2.0.2'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = 'snapshot'
                snapshot = '-DEV'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-DEV'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == '2.0.2'
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch with custom display: on tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                commit 6
                tag '2.0.2'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseMode = { nextTag, lastTag, currentTag, extension -> "${nextTag}-PREVIEW" }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-PREVIEW'
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == '2.0.2'
            assert !info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch - dirty working copy - default suffix'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
                // Nope, got to mod an existing tracked file
                // cmd 'touch', 'test.txt'
                new File(dir, 'file5') << 'Add some content'
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}-dirty" as String
            assert info.full == "feature-123-great-${headAbbreviated}-dirty" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch - dirty index - default suffix'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
                // Add a file
                new File(repo.dir, 'test.text').text = 'test'
                add 'test.txt'
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}-dirty" as String
            assert info.full == "feature-123-great-${headAbbreviated}-dirty" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch - ignored files - not dirty'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
                // Ignore file
                new File(repo.dir, '.gitignore').text = 'test.txt'
                add '.gitignore'
                commit 6
                // Add a file
                new File(repo.dir, 'test.txt').text = 'test'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}" as String
            assert info.full == "feature-123-great-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git feature branch - dirty working copy - custom suffix'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
                // Nope, need to mod an existing file to make the tree dirty
                //cmd 'touch', 'test.txt'
                new File(dir, 'file5') << 'Add some content'
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                dirtySuffix = '-dev'
                noWarningOnDirty = true
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/123-great'
            assert info.base == '123-great'
            assert info.branchId == 'feature-123-great'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-123-great-${headAbbreviated}-dev" as String
            assert info.full == "feature-123-great-${headAbbreviated}-dev" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch - dirty working copy - default'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
                // Nope, got to mod an existing file
                new File(dir, 'file5') << 'Add some content'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-dirty'
            assert info.full == "release-2.0-${headAbbreviated}-dirty" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch - dirty working copy - custom suffix'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
                // Nope, got to mod an existing file
                //cmd 'touch', 'test.txt'
                new File(dir, 'file5') << 'Mod the content'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                dirtySuffix = '-DIRTY'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-DIRTY'
            assert info.full == "release-2.0-${headAbbreviated}-DIRTY" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git shallow history for a release branch not on a tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            // Creates a temporary directory where to perform a shallow clone operation
            File detached = File.createTempDir('git', '')
            try {

                new ProcessBuilder('git', 'clone', '--depth', '1', "file://${repo.dir.absolutePath}", '.')
                        .directory(detached)
                        .start()
                        .waitForOrKill(2000L)

                def project = ProjectBuilder.builder().withProjectDir(detached).build()
                new BasicVersioningPlugin().apply(project)
                project.versioning {
                    releaseBuild = false
                }
                VersionInfo info = project.versioning.info as VersionInfo
                assert info != null
                assert info.build == headAbbreviated
                assert info.branch == 'release/2.0'
                assert info.base == '2.0'
                assert info.branchId == 'release-2.0'
                assert info.branchType == 'release'
                assert info.commit == head
                assert info.display == "2.0-SNAPSHOT" as String
                assert info.full == "release-2.0-${headAbbreviated}" as String
                assert info.scm == 'git'
                assert info.tag == null
                assert !info.dirty
                assert info.shallow
                assert info.versionNumber.versionCode == 0
                assert info.time == time
            } finally {
                detached.deleteDir()
            }
        }
    }

    @Test
    void 'Getting the version when two tags are set on a commit'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                tag '2.0.2'
                tag '2.0.3'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == "2.0.4" as String
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert !info.shallow
            assert info.versionNumber.versionCode == 20004
            assert info.time == time
        }
    }

    @Test
    void 'Git shallow history for a release branch on a tag'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            // Creates a temporary directory where to perform a shallow clone operation
            File detached = File.createTempDir('git', '')
            try {

                new ProcessBuilder('git', 'clone', '--depth', '1', "file://${repo.dir.absolutePath}", '.')
                        .directory(detached)
                        .start()
                        .waitForOrKill(2000L)

                def project = ProjectBuilder.builder().withProjectDir(detached).build()
                new BasicVersioningPlugin().apply(project)
                project.versioning {
                    releaseBuild = false
                }
                VersionInfo info = project.versioning.info as VersionInfo
                assert info != null
                assert info.build == headAbbreviated
                assert info.branch == 'release/2.0'
                assert info.base == '2.0'
                assert info.branchId == 'release-2.0'
                assert info.branchType == 'release'
                assert info.commit == head
                assert info.display == "2.0-SNAPSHOT" as String
                assert info.full == "release-2.0-${headAbbreviated}" as String
                assert info.scm == 'git'
                assert info.tag == '2.0.2'
                assert !info.dirty
                assert info.shallow
                assert info.versionNumber.versionCode == 0
                assert info.time == time
            } finally {
                detached.deleteDir()
            }
        }
    }

    @Test
    void 'Git release branch - dirty working copy - custom code'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                commit 5
                tag '2.0.2'
                commit 6
                // Nope, got to mod an existing file
                //cmd 'touch', 'test.txt'
                new File(dir, 'file5') << 'Mod the content'
            }
            def head = repo.commitLookup('Commit 6')
            def headAbbreviated = repo.commitLookup('Commit 6', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                dirty = { version -> "${version}-DONOTUSE" }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == '2.0.3-DONOTUSE'
            assert info.full == "release-2.0-${headAbbreviated}-DONOTUSE" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert info.dirty
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    @Test
    void 'Git release by tag: custom release logic'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..5).each { commit it }
                tag 'release/v2.0'
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                releaseParser = { SCMInfo gitInfo, separator = '/' ->
                    List<String> part = gitInfo.tag.split('/') + ''
                    new ReleaseInfo(part[0], part[1])
                }
                full = { SCMInfo gitInfo ->
                    "${gitInfo.tag - 'release/'}-${gitInfo.abbreviated}"
                }
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'main'
            assert info.base == 'v2.0'
            assert info.branchId == 'main'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == 'v2.0.0'
            assert info.full == "v2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == 'release/v2.0'
            assert !info.dirty
            assert info.versionNumber.versionCode == 20000
            assert info.time == time
        }
    }

    @Test
    void 'Git release branch - dirty working copy - fail'() {
        try (GitRepo repo = new GitRepo()) {
            assertThrows(DirtyException.class, () -> {
                // Git initialisation
                repo.with {
                    (1..4).each { commit it }
                    branch 'release/2.0'
                    commit 5
                    tag '2.0.2'
                    commit 6
                    // Nope, mod an existing file
                    //cmd 'touch', 'test.txt'
                    new File(dir, 'file5') << 'mod the content'
                }

                def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
                new BasicVersioningPlugin().apply(project)
                project.versioning {
                    releaseBuild = false
                    dirtyFailOnReleases = true
                }
                project.versioning.info
            })
        }
    }

    @Test
    void 'Git branch with env TEST_BRANCH'() {
        // TEST_BRANCH is provided by gradle.build
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'feature/123-great'
                commit 5
            }
            // System.setenv('TEST_BRANCH', 'feature/456-cute')
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            def project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                branchEnv << 'GIT_TEST_BRANCH'
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'feature/456-cute'
            assert info.base == '456-cute'
            assert info.branchId == 'feature-456-cute'
            assert info.branchType == 'feature'
            assert info.commit == head
            assert info.display == "feature-456-cute-${headAbbreviated}" as String
            assert info.full == "feature-456-cute-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert info.versionNumber.versionCode == 0
            assert info.time == time
        }
    }

    @Test
    void 'Custom Git directory'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                (1..4).each { commit it }
                branch 'release/2.0'
                tag '2.0.2'
                commit 5
            }
            def head = repo.commitLookup('Commit 5')
            def headAbbreviated = repo.commitLookup('Commit 5', true)
            def time = getCommitTime(repo, head)

            // Creates a temporary directory for the project
            File projectDir = File.createTempDir('project', '')
            def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
            new BasicVersioningPlugin().apply(project)
            project.versioning {
                releaseBuild = false
                gitRepoRootDir = repo.dir.absolutePath
            }
            VersionInfo info = project.versioning.info as VersionInfo
            assert info != null
            assert info.build == headAbbreviated
            assert info.branch == 'release/2.0'
            assert info.base == '2.0'
            assert info.branchId == 'release-2.0'
            assert info.branchType == 'release'
            assert info.commit == head
            assert info.display == "2.0.3" as String
            assert info.full == "release-2.0-${headAbbreviated}" as String
            assert info.scm == 'git'
            assert info.tag == null
            assert !info.dirty
            assert !info.shallow
            assert info.versionNumber.versionCode == 20003
            assert info.time == time
        }
    }

    private static String getCommitTime(GitRepo repo, String commitId) {
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(repo.dateTimeLookup(commitId))
    }
}
