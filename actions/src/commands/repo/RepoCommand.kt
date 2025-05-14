
package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.registerCloseable
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces
import kotlinx.coroutines.*
import org.kodein.di.*
import kotlin.io.path.div

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class WorkspaceRepositories(val workspace: Workspace) : AutoCloseable {
    val dispatcher by lazy { newSingleThreadContext(workspace.name) }
    val odoo = GlobalScope.async(dispatcher, start = CoroutineStart.LAZY) {
        Repository.open(workspace.path / "odoo")
    }
    val enterprise = GlobalScope.async(dispatcher, start = CoroutineStart.LAZY) {
        Repository.open(workspace.path / "enterprise")
    }
    val designThemes = GlobalScope.async(dispatcher, start = CoroutineStart.LAZY) {
        Repository.open(workspace.path / "design-themes")
    }

    override fun close() = runBlocking {
        launch(dispatcher) {
            odoo.takeIf { it.isCompleted && !it.isCancelled }?.getCompleted()?.close()
            enterprise.takeIf { it.isCompleted && !it.isCancelled }?.getCompleted()?.close()
            designThemes.takeIf { it.isCompleted && !it.isCancelled }?.getCompleted()?.close()
        }.join()
        dispatcher.close()
    }
}

class RepoCommand(
    private val workspaces: Workspaces,
) : CliktCommand() {
    override val allowMultipleSubcommands = true

    private val all by option("-a", "--all").flag()

    override fun run() = runBlocking {
        val current = workspaces.current()
        val selectedWorkspaces = if (all || current == null) workspaces.list() else listOf(current)
        val repositories = selectedWorkspaces.map { WorkspaceRepositories(it) }
        currentContext.obj = repositories
        repositories.forEach { currentContext.registerCloseable(it) }
    }
}

val REPO_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "repo") {
        add { singleton { new(::StatusCommand) } }
        add { singleton { new(::FetchCommand) } }
        add { singleton { new(::SetConflictCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::RepoCommand).subcommands(instance<Set<CliktCommand>>(tag = "repo")) } }
    }
}
