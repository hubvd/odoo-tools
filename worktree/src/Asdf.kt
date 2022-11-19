package com.github.hubvd.odootools.worktree

import com.github.ajalt.clikt.core.CliktError
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile

class Asdf(private val path: Path) {
    init {
        if (!path.isRegularFile()) throw CliktError("Invalid asdf path: $path")
    }

    context (ProcessSequenceDslContext)
    suspend fun listPlugins() = capture("$path", "plugin", "list")

    context (ProcessSequenceDslContext)
    suspend fun addPlugin(name: String) {
        run("$path", "plugin", "add", name)
    }

    context (ProcessSequenceDslContext)
    suspend fun listVersions(name: String) = capture("$path", "list", name).map { it.trim() }

    context (ProcessSequenceDslContext)
    suspend fun addVersion(name: String, version: String) {
        run("$path", "install", name, version)
    }

    context (ProcessSequenceDslContext)
    suspend fun where(name: String, version: String): Path =
        Path(capture("$path", "where", name, version).first().trim())
}
