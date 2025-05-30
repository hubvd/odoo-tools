package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.Clipboard

class OpenerCommand(private val browserService: BrowserService) : CliktCommand() {
    override fun help(context: Context) = "Opens a PR/Task"

    private val input by argument().defaultLazy { Clipboard.read(selection = true) }

    private val type by mutuallyExclusiveOptions(
        option("-t", "--ticket").flag().convert { Type.Task },
        option("-p", "--pr", "--pull-request").flag().convert { Type.PullRequest },
        option("-r", "--runbot").flag().convert { Type.RunbotError },
    ).single()

    private val all by option("-a", "--all").flag()

    override fun run() {
        if (all) {
            return when (type) {
                Type.PullRequest -> browserService.open("https://github.com/pulls")
                Type.RunbotError -> browserService.open("https://runbot.odoo.com/odoo/error")
                Type.Task, null -> browserService.open("https://www.odoo.com/odoo/49/tasks")
            }
        }

        if (type == Type.PullRequest || type == null) {
            pullRequestRe.find(input)?.let {
                val (repo, id) = it.groupValues.drop(1)
                return browserService.open("https://github.com/odoo/$repo/pull/$id")
            }
        }

        if (type == Type.RunbotError || type == null) {
            runbotErrorRe.find(input)?.groups?.get("id")?.value?.let {
                return browserService.open("https://runbot.odoo.com/odoo/runbot.build.error/$it")
            }
        }

        if (type == Type.Task || type == null) {
            val ticketId = taskRe.find(input)?.groups?.get("id")?.value ?: input.takeIf { taskIdRe.matches(input) }
            ticketId?.let {
                return browserService.open("https://www.odoo.com/odoo/49/tasks/$it")
            }
        }

        throw Abort()
    }

    private enum class Type { PullRequest, Task, RunbotError }

    companion object {
        private val pullRequestRe = Regex("""odoo/(?<repo>.*)[#:](?<id>\d+)""")
        private val taskRe = Regex("""(?i)(opw|task)[\W\-]*(?:id)?[\W-]*(?<id>\d+)""")
        private val runbotErrorRe = Regex("""(?i)runbot[\W\-]*(?:id)?[\W-]*(?<id>\d+)""")
        private val taskIdRe = Regex("""^\d+$""")
    }
}
