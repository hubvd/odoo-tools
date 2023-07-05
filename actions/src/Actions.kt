package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.kodein.di.*
import kotlin.system.exitProcess

class MainCommand : NoOpCliktCommand()

fun main(args: Array<String>) {
    val di = DI {
        bind { singleton { Config.get("workspace", WorkspaceConfig.serializer()) } }
        import(WORKSPACE_MODULE)

        import(ACTIONS_CONFIG_MODULE)

        bind { singleton { di } }
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

        bindSet {
            add {
                singleton {
                    GithubBranchLookup(
                        instance(tag = "github_api_key"),
                        instance(),
                    )
                }
            }
            add { instance(CommitRefBranchLookup) }
        }

        bind { singleton { CompositeBranchLookup(instance<Set<BranchLookup>>().toList()) } }

        bind<BrowserService> { singleton { BrowserServiceImpl(instance<ActionsConfig>().browsers) } }

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

    val mainCommand by di.instance<MainCommand>()
    val notificationService by di.instance<NotificationService>()
    val terminal by di.instance<Terminal>()

    fun echo(message: String?, error: Boolean = false) = if (error) {
        notificationService.warn(message ?: "")
    } else {
        notificationService.info(message ?: "")
    }

    try {
        mainCommand.parse(args)
    } catch (e: ProgramResult) {
        exitProcess(e.statusCode)
    } catch (e: PrintHelpMessage) {
        echo(e.command.getFormattedHelp())
        exitProcess(if (e.error) 1 else 0)
    } catch (e: PrintCompletionMessage) {
        echo(e.message)
        exitProcess(0)
    } catch (e: PrintMessage) {
        echo(e.message, e.error)
        exitProcess(if (e.error) 1 else 0)
    } catch (e: UsageError) {
        echo(e.helpMessage(), error = true)
        exitProcess(e.statusCode)
    } catch (e: CliktError) {
        echo(e.message, error = true)
        exitProcess(1)
    } catch (e: Abort) {
        exitProcess(if (e.error) 1 else 0)
    }
}
