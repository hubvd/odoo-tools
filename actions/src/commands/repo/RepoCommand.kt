package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*

class RepoCommand : NoOpCliktCommand()

val REPO_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "repo") {
        add { singleton { new(::StatusCommand) } }
        add { singleton { new(::SetConflictCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::RepoCommand).subcommands(instance<Set<CliktCommand>>(tag = "repo")) } }
    }
}
