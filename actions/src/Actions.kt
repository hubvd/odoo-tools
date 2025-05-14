package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.actions.commands.COMMANDS_MODULE
import com.github.hubvd.odootools.actions.commands.MainCommand
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.config.CONFIG_MODULE
import com.github.hubvd.odootools.odoo.client.ODOO_CLIENT_MODULE
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.kodein.di.*
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.system.exitProcess

internal val ACTION_MODULE by DI.Module {
    import(WORKSPACE_MODULE)
    bind { singleton { WorkspaceProvider(instance()).cached() } }

    import(ACTIONS_CONFIG_MODULE)
    import(COMMANDS_MODULE)
    import(RUNBOT_MODULE)
    import(CONFIG_MODULE)
    import(ODOO_CLIENT_MODULE)

    bind { singleton { new(::Odooctl) } }
    bind { singleton { Terminal() } }
    bindSingleton { new(::UserPersistence) }
    bindSingleton { EmployeeService(instance(), instance(arg = "odoo")) }
    bind<HttpHandler> { singleton { JavaHttpClient() } }

    bind<NotificationService> {
        singleton {
            instance<Terminal>().takeIf { it.terminalInfo.outputInteractive }
                ?.let { TerminalNotificationService(it) }
                ?: SystemNotificationService()
        }
    }

    bind {
        singleton {
            GithubClient(
                instance(tag = "github_api_key"),
                instance(),
            )
        }
    }

    bindSingleton { new(::DbManager) }

    bindSet {
        add { singleton { new(::GithubBranchLookup) } }
        add { instance(CommitRefBranchLookup) }
    }

    bind { singleton { CompositeBranchLookup(instance<Set<BranchLookup>>()) } }

    bind<BrowserService> { singleton { BrowserServiceImpl(instance<ActionsConfig>().browsers) } }
    bind<Git> { singleton { new(::GitShellImplementation) } }

    bind<Kitty> { singleton { Kitty() } }
}

fun main(args: Array<String>) {
    val di = DI {
        import(ACTION_MODULE)
    }

    val mainCommand by di.instance<MainCommand>()
    val notificationService by di.instance<NotificationService>()

    val arg0 = LinuxProcessHandle(ProcessHandle.current()).info().arguments().get().first()
    val exeName = Path(arg0).name
    val subcommands = mainCommand.registeredSubcommandNames()
    val symlinkedCommand = subcommands.find { it == exeName }
    val argsWithSubcommand = if (symlinkedCommand != null) listOf(symlinkedCommand, *args) else args.toList()

    fun echo(message: String, error: Boolean = false) = if (error) {
        notificationService.warn(message)
    } else {
        notificationService.info(message)
    }

    try {
        mainCommand.parse(argsWithSubcommand)
    } catch (e: CliktError) {
        getFormattedHelp(e, mainCommand, symlinkedCommand)
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                if (e is PrintCompletionMessage) {
                    println(it)
                } else {
                    echo(it, error = e.printError)
                }
            }
        exitProcess(e.statusCode)
    }
}

private fun getFormattedHelp(error: CliktError, rootCommand: CliktCommand, symlinkedCommand: String?): String? {
    if (error !is ContextCliktError) return error.message
    val ctx = error.context ?: rootCommand.currentContext
    val command = ctx.command
    val programName = ctx.commandNameWithParents()
        .let { if (symlinkedCommand == null || it.getOrNull(1) != symlinkedCommand) it else it.drop(1) }
        .joinToString(" ")
    return ctx.helpFormatter(ctx).formatHelp(
        error as? UsageError,
        command.help(ctx),
        command.helpEpilog(ctx),
        command.allHelpParams(),
        programName,
    )
}
