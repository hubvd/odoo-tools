package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.system.exitProcess

class NewCommand(private val workspaces: Workspaces, private val browserService: BrowserService) : CliktCommand(
    help = "Create a new pr for the current branch in the working directory",
) {
    override fun run() {
        val workspace = workspaces.current() ?: exitProcess(1)
        val repoPath = workspace.path.relativize(Path(System.getProperty("user.dir")))
            .subpath(0, 1)
            .takeIf { it.toString().isNotEmpty() }
            ?.takeIf { (workspace.path / it / ".git").exists() }
            ?.run { workspace.path / this }
            ?: throw Abort()

        val branch = runBlocking {
            process(
                "git",
                "-C",
                "$repoPath",
                "rev-parse",
                "--abbrev-ref",
                "HEAD",
                stdout = Redirect.CAPTURE,
                stderr = Redirect.SILENT,
            )
                .takeIf { it.resultCode == 0 }
                ?.output
                ?.firstOrNull()
        } ?: throw CliktError("Couldn't extract current branch")
        browserService.open(
            "https://github.com/odoo/${repoPath.name}/compare/${workspace.base}...odoo-dev:$branch?expand=1",
        )
    }
}
