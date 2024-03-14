package com.collibra.gradle.plugins.versioning.git

import org.junit.jupiter.api.Test

class GitInfoServiceTest {
    @Test
    void 'Git - clean'() {
        try (GitRepo repo = new GitRepo()) {
            repo.with {
                commit 1
            }
            assert !GitInfoService.isDirty(repo.status()): "Git tree clean"
        }
    }

    @Test
    void 'Git - unstaged'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                commit 1
                // Need to modify a tracked file, not just create a new untracked file
                new File(dir, 'file1') << 'Add some content'
            }
            assert GitInfoService.isDirty(repo.status()): "Unstaged changes"
        }
    }

    @Test
    void 'Git - uncommitted'() {
        try (GitRepo repo = new GitRepo()) {
            // Git initialisation
            repo.with {
                commit 1
                // Add a file, without committing it
                new File(repo.dir, 'test.txt').text = 'Test'
                add 'test.txt'
            }
            assert GitInfoService.isDirty(repo.status()): "Uncommitted changes"
        }
    }
}
