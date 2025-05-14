package com.github.hubvd.odootools.actions.commands.odooctl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*

class OdooctlCommand() : NoOpCliktCommand()

val ODOOCTL_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "odooctl") {
        add { singleton { new(::ListCommand) } }
        add { singleton { new(::KillCommand) } }
        add { singleton { new(::CurrentCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::OdooctlCommand).subcommands(instance<Set<CliktCommand>>(tag = "odooctl")) } }
    }
}
