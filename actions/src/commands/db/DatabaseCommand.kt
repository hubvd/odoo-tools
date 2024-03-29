package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*

class DatabaseCommand : NoOpCliktCommand(
    name = "db",
)

val DATABASE_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "db") {
        add { singleton { new(::ListCommand) } }
        add { singleton { new(::SaveCommand) } }
        add { singleton { new(::RestoreCommand) } }
        add { singleton { new(::DropCommand) } }
        add { singleton { new(::CopyCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::DatabaseCommand).subcommands(instance<Set<CliktCommand>>(tag = "db")) } }
    }
}
