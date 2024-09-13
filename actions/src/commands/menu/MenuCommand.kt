package com.github.hubvd.odootools.actions.commands.menu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*
import org.kodein.di.DI
import org.kodein.di.bindSet

class MenuCommand() : NoOpCliktCommand()

val MENU_COMMAND_MODULE by DI.Module {
    bindSet<CliktCommand>(tag = "menu") {
        add { singleton { new(::AddonCommand) } }
    }

    inBindSet<CliktCommand> {
        add { singleton { new(::MenuCommand).subcommands(instance<Set<CliktCommand>>(tag = "menu")) } }
    }
}
