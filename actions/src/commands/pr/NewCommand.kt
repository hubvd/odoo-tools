package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.hubvd.odootools.actions.git.currentRepository
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.name

class NewCommand(private val workspaces: Workspaces, private val browserService: BrowserService) : CliktCommand() {
    override fun help(context: Context) = "Create a new pr for the current branch in the working directory"

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()
        val repo = workspace.currentRepository() ?: throw Abort()
        val branch = repo.use {
            it.head().branchName()
        } ?: throw CliktError("Couldn't extract current branch")
        browserService.open(
            "https://github.com/odoo/${repo.path.name}/compare/${workspace.base}...odoo-dev:$branch?expand=1",
        )
    }
}
