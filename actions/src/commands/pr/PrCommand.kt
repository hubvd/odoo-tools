package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*

class PrCommand : NoOpCliktCommand(help = "Work with pull requests")

val PR_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "pr") {
        add { singleton { new(::NewCommand) } }
        add { singleton { new(::ListCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::PrCommand).subcommands(instance<Set<CliktCommand>>(tag = "pr")) } }
    }
}
