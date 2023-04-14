package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import kotlinx.serialization.Serializable
import org.kodein.di.*

class MainCommand : NoOpCliktCommand()

@Serializable
data class ActionsConfig(val password: String)

fun main(args: Array<String>) {
    val di = DI {
        bind { singleton { Config.get("workspace", WorkspaceConfig.serializer()) } }
        bind { singleton { Config.get("actions", ActionsConfig.serializer()) } }
        import(WORKSPACE_MODULE)

        bind { singleton { di } }
        bind { singleton { new(::Odooctl) } }
        bind { singleton { Terminal() } }

        bindSet {
            add { singleton { new(::PycharmCommand) } }
            add { singleton { new(::OpenCommand) } }
            add { singleton { new(::RunTestCommand) } }
            add { singleton { new(::AttachCommand) } }
            add { singleton { new(::OdooctlCommand) } }
            add { singleton { new(::RestoreCommand) } }
        }
    }

    val subcommands by di.instance<Set<CliktCommand>>()
    MainCommand().subcommands(
        *subcommands.toTypedArray(),
    ).main(args)
}
