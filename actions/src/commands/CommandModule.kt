package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.hubvd.odootools.actions.commands.pr.PR_COMMAND_MODULE
import com.github.hubvd.odootools.actions.commands.pycharm.PYCHARM_ACTIONS_MODULE
import org.kodein.di.*

class MainCommand : NoOpCliktCommand(name = "actions")

val COMMANDS_MODULE = DI.Module("Commands") {
    bindSet {
        add { singleton { new(::PycharmCommand) } }
        add { singleton { new(::OpenCommand) } }
        add { singleton { new(::AttachCommand) } }
        add { singleton { new(::OdooctlCommand) } }
        add {
            singleton {
                RestoreCommand(
                    terminal = instance(),
                    dumpPassword = instance(tag = "odoo_dump_password"),
                    httpHandler = instance(),
                )
            }
        }
        add { singleton { new(::QrCommand) } }
        add { singleton { new(::CheckoutCommand) } }
        add { singleton { new(::NewCommand) } }
        add { singleton { new(::BisectCommand) } }
    }
    import(PR_COMMAND_MODULE)
    import(PYCHARM_ACTIONS_MODULE)

    bind { singleton { MainCommand().subcommands(instance<Set<CliktCommand>>()) } }
}
