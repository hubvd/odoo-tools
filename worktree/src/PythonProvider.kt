@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package com.github.hubvd.odootools.worktree

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

interface PythonProvider {
    context(ProcessSequenceDslContext)
    suspend fun installOrGetVersion(version: String): Path
}

fun pythonProvider(): PythonProvider {
    val systemPaths = System.getenv("PATH").split(':').map { Path(it) }

    fun findExe(paths: List<Path>, exe: String) = paths
        .asSequence()
        .map { it / exe }
        .filter { it.exists() }
        .find { it.isExecutable() }

    return findExe(listOf(Path("/opt/asdf-vm/bin")) + systemPaths, "asdf")?.let { AsdfPythonProvider(it) }
        ?: findExe(systemPaths, "pyenv")?.let { PyenvPythonProvider(it) }
        ?: SystemPythonProvider()
}

class SystemPythonProvider : PythonProvider {
    context(ProcessSequenceDslContext)
    override suspend fun installOrGetVersion(version: String) = Path(capture("which", "python3").first().trim())
}

class PyenvPythonProvider(private val path: Path) : PythonProvider {
    context(ProcessSequenceDslContext)
    override suspend fun installOrGetVersion(version: String): Path {
        if (version !in listVersions()) installVersion(version)
        return Path(capture("$path", "root").first().trim()) / "versions" / version / "bin/python"
    }

    context(ProcessSequenceDslContext)
    private suspend fun installVersion(version: String) {
        run("$path", "install", version, description = "Installing python $version")
    }

    context(ProcessSequenceDslContext)
    private suspend fun listVersions() = capture("$path", "versions")
        .map { it.drop(2).takeWhile { !it.isWhitespace() } }
        .filterNot { it == "system" }
}

class AsdfPythonProvider(private val path: Path) : PythonProvider {

    context(ProcessSequenceDslContext)
    override suspend fun installOrGetVersion(version: String): Path {
        if ("python" !in listPlugins()) addPlugin("python")
        if (version !in listVersions("python")) addVersion("python", version)
        return where("python", version) / "bin/python"
    }

    context (ProcessSequenceDslContext)
    private suspend fun listPlugins() = capture("$path", "plugin", "list")

    context (ProcessSequenceDslContext)
    private suspend fun addPlugin(name: String) {
        run("$path", "plugin", "add", name)
    }

    context (ProcessSequenceDslContext)
    private suspend fun listVersions(name: String) = capture("$path", "list", name).map { it.trim() }

    context (ProcessSequenceDslContext)
    private suspend fun addVersion(name: String, version: String) {
        run("$path", "install", name, version)
    }

    context (ProcessSequenceDslContext)
    private suspend fun where(name: String, version: String): Path =
        Path(capture("$path", "where", name, version).first().trim())
}
