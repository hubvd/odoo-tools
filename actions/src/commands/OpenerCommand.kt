package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.Clipboard

class OpenerCommand(private val browserService: BrowserService) : CliktCommand(help = "Opens a PR/Task") {
    private val input by argument().defaultLazy { Clipboard.read(selection = true) }

    private val type by mutuallyExclusiveOptions(
        option("-t", "--ticket").flag().convert { Type.Task },
        option("-p", "--pr", "--pull-request").flag().convert { Type.PullRequest },
    ).single()

    private val all by option("-a", "--all").flag()

    override fun run() {
        if (all) {
            @Suppress("ktlint:standard:max-line-length")
            return when (type) {
                Type.PullRequest -> browserService.open("https://github.com/pulls")
                Type.Task, null -> browserService.open(
                    "https://www.odoo.com/web#action=333&active_id=49&model=project.task&view_type=kanban&cids=1&menu_id=4720",
                )
            }
        }

        if (type == Type.PullRequest || type == null) {
            pullRequestRe.find(input)?.let {
                val (repo, id) = it.groupValues.drop(1)
                return browserService.open("https://github.com/odoo/$repo/pull/$id")
            }
        }

        if (type == Type.Task || type == null) {
            val ticketId = taskRe.find(input)?.groups?.get("id")?.value ?: input.takeIf { taskIdRe.matches(input) }
            ticketId?.let {
                return browserService.open("https://www.odoo.com/web#view_type=form&model=project.task&id=$it")
            }
        }

        throw Abort()
    }

    private enum class Type { PullRequest, Task }

    companion object {
        private val pullRequestRe = Regex("""odoo/(?<repo>.*)#(?<id>\d+)""")
        private val taskRe = Regex("""(?i)(opw|task)[\W\-]*(?:id)?[\W-]*(?<id>\d+)""")
        private val taskIdRe = Regex("""^\d+$""")
    }
}
