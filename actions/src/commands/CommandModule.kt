package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.hubvd.odootools.actions.commands.db.DATABASE_COMMAND_MODULE
import com.github.hubvd.odootools.actions.commands.odooctl.KillCommand
import com.github.hubvd.odootools.actions.commands.odooctl.ODOOCTL_COMMAND_MODULE
import com.github.hubvd.odootools.actions.commands.pr.PR_COMMAND_MODULE
import com.github.hubvd.odootools.actions.commands.pycharm.PYCHARM_ACTIONS_MODULE
import com.github.hubvd.odootools.actions.commands.repo.REPO_COMMAND_MODULE
import org.kodein.di.*

class MainCommand : NoOpCliktCommand(name = "actions")

val COMMANDS_MODULE = DI.Module("Commands") {
    bindSet {
        add { singleton { new(::PycharmCommand) } }
        add { singleton { new(::OpenCommand) } }
        add { singleton { new(::AttachCommand) } }
        add {
            singleton {
                RestoreCommand(
                    dumpPassword = instance(tag = "odoo_dump_password"),
                    httpHandler = instance(),
                )
            }
        }
        add { singleton { new(::QrCommand) } }
        add { singleton { new(::CheckoutCommand) } }
        add { singleton { new(::NewCommand) } }
        add { singleton { new(::BisectCommand) } }
        add { singleton { new(::OpenerCommand) } }
        add { singleton { new(::WhoIsCommand) } }
        add { singleton { IsOffCommand(instance(arg = "odoo")) } }
        add { singleton { ActivitiesCommand(instance(arg = "odoo"), instance()) } }
        add { singleton { new(::LintCommand) } }
        add { singleton { new(::DoneCommand) } }
    }

    bind { singleton { new(::KillCommand) } }

    import(PR_COMMAND_MODULE)
    import(PYCHARM_ACTIONS_MODULE)
    import(DATABASE_COMMAND_MODULE)
    import(REPO_COMMAND_MODULE)
    import(ODOOCTL_COMMAND_MODULE)

    bind { singleton { MainCommand().subcommands(instance<Set<CliktCommand>>()) } }
}
