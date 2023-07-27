package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.kodein.di.*

class MainCommand : NoOpCliktCommand(name = "actions")

val COMMANDS_MODULE = DI.Module("Commands") {
    bindSet {
        add { singleton { new(::PycharmCommand) } }
        add { singleton { new(::OpenCommand) } }
        add { singleton { new(::RunTestCommand) } }
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
        add { singleton { new(::OpenGitCommand) } }
        add { singleton { new(::QrCommand) } }
        add { singleton { new(::CheckoutCommand) } }
        add { singleton { new(::NewCommand) } }
    }

    bind { singleton { MainCommand().subcommands(instance<Set<CliktCommand>>()) } }
}
