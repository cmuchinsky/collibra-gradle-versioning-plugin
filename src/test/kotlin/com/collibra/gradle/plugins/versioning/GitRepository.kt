package com.collibra.gradle.plugins.versioning

import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId

internal class GitRepository : AutoCloseable {
    val dir: File = Files.createTempDirectory("git").toFile()
    private val git: Git = Git.init().setDirectory(dir).call()

    override fun close() {
        try {
            git.close()
        } finally {
            dir.deleteRecursively()
        }
    }

    fun commit(no: Int) {
        val fileName = "file$no"
        dir.resolve(fileName).writeText("Text for commit $no")
        git.add().addFilepattern(fileName).call()
        git.commit().setMessage("Commit $no").call()
    }

    fun add(vararg paths: String) {
        paths.forEach { git.add().addFilepattern(it).call() }
    }

    fun branch(name: String) {
        git.checkout().setName(name).setCreateBranch(true).call()
    }

    fun checkout(name: String) {
        git.checkout().setName(name).call()
    }

    fun tag(name: String) {
        git.tag().setName(name).call()
    }

    fun commitLookup(message: String, abbreviated: Boolean = false): String {
        return git.log()
            .call()
            .filter { it.fullMessage.contains(message) }
            .map { commit ->
                if (abbreviated) {
                    git.repository.newObjectReader().use { it.abbreviate(commit).name() }
                } else {
                    ObjectId.toString(commit)
                }
            }
            .first() ?: throw RuntimeException("Cannot find commit for message $message")
    }

    fun commitTime(commitId: String): String {
        return git.log()
            .call()
            .filter { ObjectId.toString(it) == commitId }
            .map { commit ->
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(commit.commitTime.toLong()),
                        commit.committerIdent.timeZone?.toZoneId() ?: ZoneOffset.UTC
                    )
                )
            }
            .first() ?: throw RuntimeException("Cannot find commit for ID $commitId")
    }

    override fun toString(): String {
        return dir.toString()
    }
}
