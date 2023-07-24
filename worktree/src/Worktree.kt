package com.github.hubvd.odootools.worktree

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import com.github.hubvd.odootools.worktree.commands.COMMAND_MODULE
import org.kodein.di.*
import kotlin.io.path.Path
import kotlin.io.path.div

class WorktreeCommand : NoOpCliktCommand() {
    override fun aliases() = mapOf(
        "version" to listOf("current", "version"),
        "path" to listOf("current", "path"),
        "base" to listOf("current", "base"),
        "name" to listOf("current", "name"),
        "paths" to listOf("list", "path"),
        "names" to listOf("list", "name"),
    )
}

object DataDir {
    private val BASE = Path(
        System.getenv("XDG_DATA_DIRS") ?: (System.getProperty("user.home") + "/.local/share"),
    ) / "odoo-tools"

    operator fun get(name: String) = BASE / name
}

fun main(args: Array<String>) {
    val di = DI {
        bind { singleton { WorkspaceProvider(instance()).cached() } }
        bind { singleton { pythonProvider() } }
        bind { singleton { Terminal() } }
        bind { singleton { new(::OdooStubs) } }
        bind { singleton { new(::Virtualenvs) } }
        bind { singleton { DataDir } }
        import(COMMAND_MODULE)
        import(WORKSPACE_MODULE)
    }

    val subcommands by di.instance<Set<CliktCommand>>()
    WorktreeCommand().subcommands(
        *subcommands.toTypedArray(),
    ).main(args)
}
