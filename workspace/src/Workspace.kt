package com.github.hubvd.odootools.workspace

import com.github.hubvd.odootools.config.ShellPath
import com.github.hubvd.odootools.workspace.WorkspaceFormat.*
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*
import kotlinx.serialization.json.Json as Jsonx

@Serializable
data class WorkspaceConfig(val root: ShellPath, val default: String)

@Serializable
data class Workspace(val name: String, val path: ShellPath) {

    val version: Float
    val base: String

    init {
        val release = readRelease()
        version = release.first
        base = release.second
    }

    private fun readRelease(): Pair<Float, String> {
        val txt = (path / "odoo/odoo/release.py").readText()
        val (major, minor, level) = versionRe.find(txt)!!.destructured
        val majorNumber = major.removePrefix("saas~")
        val version = "$majorNumber.$minor".toFloat()
        val base = if (level != "FINAL")
            "master"
        else
            "${major.replace('~', '-')}.$minor"
        return version to base
    }

    override fun toString(): String {
        return "Workspace(name=$name, path=$path, version=$version, base=$base)"
    }

    companion object {
        private val versionRe =
            """version_info = ?\(['"]?((?:saas~)?\d+)['"]?,\s*(\d+),\s*\d+,\s*([A-Z]+)""".toRegex()
    }

}

class Workspaces(private val config: WorkspaceConfig) {
    private fun listWorktrees(repository: Path): List<Path> {
        val dotGit = repository / ".git"
        val attributes = try {
            dotGit.readAttributes<BasicFileAttributes>()
        } catch (e: NoSuchFileException) {
            error("Repo $repository does not exists")
        }
        val mainRepository = when {
            attributes.isRegularFile -> Path(
                dotGit.readText().trimEnd().removePrefix("gitdir: ")
            ).parent.parent.parent

            attributes.isDirectory -> repository
            else -> error("???")
        }
        val worktrees = (mainRepository / ".git/worktrees").toFile().listFiles() ?: emptyArray()
        return buildList(capacity = worktrees.size + 1) {
            add(mainRepository)
            worktrees.forEach {
                Path((it.toPath() / "gitdir").readText().trimEnd())
                    .takeIf { it.exists() }
                    ?.parent
                    ?.let { add(it) }
            }
        }
    }

    fun list() = listWorktrees(config.root / config.default / "odoo")
        .map { it.parent }
        .map { Workspace(it.name, it) }
        .sortedBy { it.name.removePrefix("saas-") }

    fun default() = list().find { it.name == config.default }!!

    fun current() = Path(System.getProperty("user.dir")).let { cwd -> list().find { cwd.startsWith(it.path) } }

}

enum class WorkspaceFormat { Name, Version, Path, Base, Json, Fish }

fun Workspace.format(format: WorkspaceFormat): String = when (format) {
    Name -> name
    Version -> version.toString()
    Path -> path.toString()
    Base -> base
    Json -> Jsonx.encodeToString(Workspace.serializer(), this)
    Fish -> buildString {
        for ((name, value) in arrayOf(
            "version" to version,
            "name" to name,
            "path" to path,
            "base" to base,
        )) {
            append("set workspace_")
            append(name)
            append(' ')
            append(value)
            append(';')
        }
    }
}

val workspaceModule = DI.Module("workspace") {
    bind { singleton { Workspaces(instance()) } }
}
