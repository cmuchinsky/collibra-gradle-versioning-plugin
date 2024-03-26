package com.collibra.gradle.plugins.versioning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GitRepositoryInfoTest {
    @Test
    fun `Test git compute`() {
        GitRepository().use { repo ->
            // given
            with(repo) { (1..4).forEach { commit(it) } }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val branchName = "main"

            // when
            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            project.pluginManager.apply(VersioningPlugin::class.java)
            val extension = project.extensions.getByType(VersioningExtension::class.java)
            GitRepositoryInfo.openGit(project, extension).use { git ->
                val tagRefs = GitRepositoryInfo.getTagRefs(git)
                val tagNames = GitRepositoryInfo.getTagNames(git, tagRefs, branchName)

                // then
                val info = GitRepositoryInfo.buildInfo(git, branchName, tagRefs, tagNames)
                assertEquals(
                    GitRepositoryInfo(
                        branchName,
                        head,
                        headAbbreviated,
                        info.dateTime,
                        null,
                        null,
                        info.status,
                        false,
                        false
                    ),
                    info
                )
            }
        }
    }
}
