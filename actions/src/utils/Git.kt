package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.workspace.Workspace
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.github.pgreze.process.unwrap
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.div

interface Git {
    fun open(path: Path): LegacyRepository
}

context(Git)
fun Workspace.odoo() = open(path / "odoo")

context(Git)
fun Workspace.enterprise() = open(path / "enterprise")

data class Commit(val hash: String, val title: String)

interface LegacyRepository {
    fun commitTitle(hash: String): String
    fun switch(hash: String)
    fun commitsBetween(oldHash: String, newHash: String): List<Commit>
}

internal class GitShellImplementation : Git {
    override fun open(path: Path): LegacyRepository = LegacyRepositoryShellImplementation(path)
}

private class LegacyRepositoryShellImplementation(private val path: Path) : LegacyRepository {
    override fun commitTitle(hash: String) = runBlocking {
        process(
            "git",
            "-C",
            "$path",
            "log",
            "--pretty=format:%s",
            "-1",
            hash,
            stdout = Redirect.CAPTURE,
            stderr = Redirect.SILENT,
        ).unwrap().firstOrNull()?.trim() ?: ""
    }

    override fun switch(hash: String) {
        runBlocking {
            process(
                "git",
                "-C",
                "$path",
                "switch",
                "--detach",
                hash,
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            ).unwrap()
        }
    }

    override fun commitsBetween(oldHash: String, newHash: String): List<Commit> = if (oldHash == newHash) {
        listOf(Commit(newHash, commitTitle(newHash)))
    } else {
        runBlocking {
            // includes newRef

            // what about merge commits ?
            process(
                "git",
                "-C",
                "$path",
                "log",
                "--pretty=format:%H %s",
                "$oldHash..$newHash",
                stdout = Redirect.CAPTURE,
                stderr = Redirect.SILENT,
            )
                .unwrap()
                .map { Commit(it.substringBefore(' '), it.substringAfter(' ')) }
        }
    }
}
