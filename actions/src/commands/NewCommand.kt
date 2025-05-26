package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.hubvd.odootools.actions.ActionsConfig
import com.github.hubvd.odootools.actions.utils.Clipboard
import com.github.hubvd.odootools.actions.utils.NotificationService
import com.github.hubvd.odootools.actions.utils.currentRepository
import com.github.hubvd.odootools.workspace.Workspaces

class NewCommand(
    private val workspaces: Workspaces,
    private val notificationService: NotificationService,
    private val config: ActionsConfig,
) : CliktCommand() {
    override fun help(context: Context) = "Create or switch to a branch with the selected id"

    private val type by mutuallyExclusiveOptions(
        option("-s", "--sentry").flag().convert { "sentry" },
        option("-t", "--task").flag().convert { "opw" },
        option("-r", "--runbot").flag().convert { "runbot" },
    ).default("opw")

    private val id by argument().int().defaultLazy {
        Clipboard.read().let { value ->
            value.toIntOrNull()
                ?: throw BadParameterValue(
                    currentContext.localization.intConversionError(value),
                    registeredArguments().first { it.name == "ID" },
                )
        }
    }

    private val description by argument().optional()

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()
        val repo = workspace.currentRepository() ?: throw Abort()

        val branch = buildString {
            append(workspace.base)
            append('-')
            append(type)
            append('-')
            append(id)
            description?.let {
                append('-')
                append(it.replace(' ', '_').replace('-', '_'))
            }
            append('-')
            append(config.trigram)
        }

        repo.use {
            var branchRef = it.findBranch(branch)
            if (branchRef != null) {
                notificationService.info("$branch already exists", "switching to it")
            } else {
                branchRef = it.createBranch(branch, it.head().target()!!.commit())
            }
            it.checkoutBranch(branchRef)
        }
    }
}
