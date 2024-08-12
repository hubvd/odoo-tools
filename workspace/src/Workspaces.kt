package com.github.hubvd.odootools.workspace

import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

interface Workspaces {
    fun list(): List<Workspace>
    fun default(): Workspace
    fun current(): Workspace?
}

abstract class BaseWorkspaceProvider : Workspaces {
    abstract val defaultName: String

    override fun current(): Workspace? =
        Path(System.getProperty("user.dir")).let { cwd -> list().find { cwd.startsWith(it.path) } }

    override fun default() = list().find { it.name == defaultName }!!
}

internal class CachedWorkspaces(override val defaultName: String, private val workspaces: Workspaces) :
    BaseWorkspaceProvider() {
    private val list: List<Workspace> by lazy { workspaces.list() }
    override fun list(): List<Workspace> = list
}

class WorkspaceProvider(private val config: WorkspaceConfig) : BaseWorkspaceProvider() {
    private fun listWorktrees(repository: Path): List<Path> {
        val dotGit = repository / ".git"
        val attributes = try {
            dotGit.readAttributes<BasicFileAttributes>()
        } catch (e: NoSuchFileException) {
            error("Repo $repository does not exists")
        }
        val mainRepository = when {
            attributes.isRegularFile -> Path(
                dotGit.readText().trimEnd().removePrefix("gitdir: "),
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

    override val defaultName: String
        get() = config.default

    override fun list(): List<Workspace> = listWorktrees(config.root / config.default / "odoo")
        .filter { it.name == "odoo" }
        .map { it.parent }
        .map { WorkspaceImpl(it.name, it) }
        .sortedWith(compareBy({ it.version }, { it.name }))

    fun cached(): Workspaces = CachedWorkspaces(config.default, this)
}
