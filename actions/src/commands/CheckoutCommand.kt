package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.Redirect.CAPTURE
import com.github.pgreze.process.Redirect.SILENT
import com.github.pgreze.process.process
import kotlinx.coroutines.*
import java.io.File

class CheckoutCommand(
    private val workspaces: Workspaces,
    private val terminal: Terminal,
    private val notificationService: NotificationService,
    private val branchLookup: BranchLookup,
    private val kitty: Kitty,
) : CliktCommand(
    help = "Checkout a pull request from a github url or a commit ref (remote:branch)",
) {
    private val branch by argument()
        .defaultLazy { runBlocking { process("wl-paste", stdout = CAPTURE).output.first() } }

    override fun run() {
        val ref = branchLookup(branch)
            ?: throw CliktError("Couldn't extract base branch")

        var workspace = workspaces.current()
        if (workspace != null && workspace.base != ref.base) {
            throw CliktError("Current workspace has wrong base")
        } else if (workspace == null) {
            workspace = workspaces.list().find { it.base == ref.base && it.name == ref.base }
        }

        if (workspace == null) {
            throw CliktError("Workspace not found")
        }

        notificationService.info("Fetching ${ref.branch}", "from ${ref.remote}")

        runBlocking {
            val (odoo, enterprise) = awaitAll(
                async { fetchAndCheckout(ref, workspace.path.resolve("odoo").toFile()) },
                async { fetchAndCheckout(ref, workspace.path.resolve("enterprise").toFile()) },
            )
            if (!terminal.info.outputInteractive) {
                kitty.openGit(workspace, odoo, enterprise)
            }
        }
    }

    private suspend fun fetchAndCheckout(ref: BranchRef, workDir: File): Boolean {
        val remoteRe = Regex("""(\w+)\s+(\S+)""")

        val remote = process("git", "remote", "-v", directory = workDir, stderr = SILENT, stdout = CAPTURE)
            .output
            .filter { it.endsWith("(fetch)") }
            .mapNotNull { remoteRe.find(it)?.groups?.let { it[1]!!.value to it[2]!!.value } }
            .filter { it.second.contains(ref.remote) }
            .find { (_, remote) -> ref.remote == remote.substring(remote.indexOf(ref.remote)).takeWhile { it != '/' } }
            ?.first
            ?: throw CliktError("Couldn't find remote ${ref.remote}")

        val fetchResult = process("git", "fetch", remote, ref.branch, directory = workDir, stderr = SILENT)
        if (fetchResult.resultCode == 128) {
            return false
        } else if (fetchResult.resultCode != 0) {
            throw CliktError("git exited with ${fetchResult.resultCode} while fetching $remote:${ref.branch}")
        }

        val currentBranch = process(
            "git",
            "branch",
            "--show-current",
            directory = workDir,
            stdout = CAPTURE,
            stderr = SILENT,
        ).output.firstOrNull()

        when (currentBranch) {
            ref.branch -> return true
            ref.base -> {}
            else -> {
                notificationService.warn("Checkout skipped", "$currentBranch is active")
                return true
            }
        }

        val hasStagedFiles = process(
            "git",
            "status",
            "--porcelain",
            directory = workDir,
            stdout = CAPTURE,
            stderr = SILENT,
        ).output.any { it.firstOrNull() in arrayOf('A', 'M', 'R', 'D', 'C') }

        if (hasStagedFiles) {
            notificationService.warn("Checkout skipped", "$currentBranch is dirty")
            return true
        }

        process("git", "checkout", ref.branch, directory = workDir)
        return true
    }
}
