package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.kitty.Window
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.Path
import kotlin.io.path.name

abstract class BaseCopyCommand(
    private val dbManager: DbManager,
    private val odooctl: Odooctl,
    private val kitty: Kitty,
    private val notificationService: NotificationService,
    private val workspaces: Workspaces,
) : CliktCommand() {

    private val running by option("--running", "-r").flag()
    private val current by option("--current", "-c").flag()
    private val database by argument(
        completionCandidates = CompletionCandidates.Custom.databases(includeSavepoints = false),
    ).optional()
    private val instances by lazy { odooctl.instances() }

    private fun selectDatabase() = when {
        database != null -> database
        running -> selectInstance(instances)?.database
        current -> workspaces.current()?.name
        else -> dbManager.list(includeSavepoints = false)?.let {
            menu(it)
        }
    }

    abstract fun fromTo(database: String): Pair<String, String>

    override fun run() {
        val database = selectDatabase() ?: throw Abort()
        val instance = instances.find { it.database == database }
        var window: Window? = null
        if (instance != null) {
            window = findWindow(instance)
            odooctl.kill(instance)?.get()
        }

        val (from, to) = fromTo(database)
        dbManager.copy(from, to)
        if (instance != null && window != null) {
            restartOdoo(window, instance)
        }
        if (!terminal.terminalInfo.interactive) {
            notificationService.info(notificationMessage(database))
        }
    }

    private fun findWindow(instance: OdooInstance): Window? = if (kitty.isRunning()) {
        kitty.ls().flatMap { it.tabs }
            .flatMap { it.windows }
            .find { it.foregroundProcesses.any { it.pid == instance.pid } }
    } else {
        null
    }

    private fun restartOdoo(window: Window, instance: OdooInstance) {
        kitty.focusWindow("id:${window.id}")
        val process = window.foregroundProcesses.first { it.cmdline?.firstOrNull()?.let(::Path)?.name == "odoo" }
        kitty.launch(
            *process.cmdline!!.filter { it != "--restart" }.toTypedArray(),
            "--restart",
            cwd = instance.workspace.path.toString(),
            type = "overlay-main",
            hold = true,
        )
        kitty.closeWindow("id:${window.id}")
    }

    abstract fun notificationMessage(database: String): String
}
