package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.hubvd.odootools.actions.utils.NotificationService
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking

class NewCommand(
    private val workspaces: Workspaces,
    private val notificationService: NotificationService,
) : CliktCommand() {
    private val isSentry by option("-s", "--sentry").flag()
    private val id by argument().int()
    private val description by argument().optional()

    override fun run() {
        val worktree = workspaces.current() ?: throw Abort()
        val branch = buildString {
            append(worktree.base)
            append('-')
            if (isSentry) append("sentry") else append("opw")
            append('-')
            append(id)
            description?.let {
                append('-')
                append(it.replace(' ', '_').replace('-', '_'))
            }
            append("-huvw")
        }
        runBlocking {
            val exists = process(
                "git",
                "rev-parse",
                "--quiet",
                "--verify",
                branch,
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            ).resultCode == 0

            if (exists) notificationService.info("$branch already exists", "switching to it")

            process(
                "git",
                "checkout",
                *(if (exists) emptyArray() else arrayOf("-b")),
                branch,
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            )
        }
    }
}
