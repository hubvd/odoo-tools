package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.actions.commands.COMMANDS_MODULE
import com.github.hubvd.odootools.actions.commands.MainCommand
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.config.CONFIG_MODULE
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.kodein.di.*
import kotlin.system.exitProcess

internal val ACTION_MODULE by DI.Module {
    import(WORKSPACE_MODULE)
    bind { singleton { WorkspaceProvider(instance()).cached() } }

    import(ACTIONS_CONFIG_MODULE)
    import(COMMANDS_MODULE)
    import(RUNBOT_MODULE)
    import(CONFIG_MODULE)

    bind { singleton { new(::Odooctl) } }
    bind { singleton { Terminal() } }
    bind<HttpHandler> { singleton { JavaHttpClient() } }

    bind<NotificationService> {
        singleton {
            instance<Terminal>().takeIf { it.info.outputInteractive }
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

    bindSet {
        add { singleton { new(::GithubBranchLookup) } }
        add { instance(CommitRefBranchLookup) }
    }

    bind { singleton { CompositeBranchLookup(instance<Set<BranchLookup>>()) } }

    bind<BrowserService> { singleton { BrowserServiceImpl(instance<ActionsConfig>().browsers) } }
    bind<Git> { singleton { new(::GitShellImplementation) } }

    bind<WindowManager> { singleton { new(WindowManager.Companion::invoke) } }
    bind<Kitty> { singleton { Kitty(instance()) } }
}

fun main(args: Array<String>) {
    val di = DI {
        import(ACTION_MODULE)
    }

    val mainCommand by di.instance<MainCommand>()
    val notificationService by di.instance<NotificationService>()
    val terminal by di.instance<Terminal>()

    fun echo(message: String, error: Boolean = false) = if (error) {
        notificationService.warn(message)
    } else {
        notificationService.info(message)
    }

    try {
        mainCommand.parse(args)
    } catch (e: CliktError) {
        getFormattedHelp(e, mainCommand)
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

private fun getFormattedHelp(error: CliktError, rootCommand: CliktCommand): String? {
    if (error !is ContextCliktError) return error.message
    val ctx = error.context ?: rootCommand.currentContext
    val command = ctx.command
    val programName = ctx.commandNameWithParents()
        .let { if (it.size == 1) it else it.drop(1) }
        .joinToString(" ")
    return ctx.helpFormatter(ctx).formatHelp(
        error as? UsageError,
        command.commandHelp(ctx),
        command.commandHelpEpilog(ctx),
        command.allHelpParams(),
        programName,
    )
}
