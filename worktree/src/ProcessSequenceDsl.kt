package com.github.hubvd.odootools.worktree

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.ajalt.mordant.widgets.progress.text
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.github.pgreze.process.unwrap
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

private const val CSI = "\u001B["

class ProcessSequenceDslContext(private val terminal: Terminal) : AutoCloseable {
    private var cwd: File = File(System.getProperty("user.dir"))

    private val progress = MultiProgressBarAnimation(terminal).animateOnThread()

    private val currentTaskProgress = progressBarContextLayout(alignColumns = false) {
        spinner(Spinner.Dots())
        text { context }
    }

    private val processOutputProcess = progressBarContextLayout(alignColumns = false) {
        text(fps = 60) { context }
    }

    private val currentTask = progress.addTask(currentTaskProgress, "")
    private val outputTask = progress.addTask(processOutputProcess, "")

    private fun Terminal.disableLineWrap() = rawPrint("$CSI?7l")
    private fun Terminal.enableLineWrap() = rawPrint("$CSI?7h")

    init {
        terminal.cursor.hide(showOnExit = true)
        terminal.disableLineWrap()
        progress.execute()
    }

    fun cd(path: Path) {
        cwd = path.toFile()
    }

    private fun stepStart(description: String) {
        currentTask.update { context = description }
    }

    private fun stepSuccess(description: String) {
        terminal.println("${green("✓")}  $description")
    }

    fun step(description: String, block: () -> Unit) {
        stepStart(description)
        block()
        stepSuccess(description)
    }

    suspend fun capture(vararg cmd: String): List<String> =
        process(*cmd, stdout = Redirect.CAPTURE, stderr = Redirect.CAPTURE).unwrap()

    suspend fun run(vararg cmd: String, description: String? = null, env: Map<String, String>? = null) {
        val taskDescription = description ?: (blue + underline)(
            Path(cmd[0]).name + cmd.drop(1)
                .joinToString(" ", prefix = " "),
        )
        stepStart(taskDescription)
        val result =
            process(*cmd, directory = cwd, stdout = Redirect.CAPTURE, stderr = Redirect.CAPTURE, env = env) { line ->
                outputTask.update { context = line.trim() }
            }

        if (result.resultCode != 0) {
            val errorMessage = buildString {
                append(brightRed(cmd.joinToString(" ")))
                append(red(" failed with exit code "))
                append(yellow(result.resultCode.toString()))
                appendLine()
                result.output.forEach {
                    appendLine(gray(it))
                }
            }

            terminal.println(errorMessage)
            close()
            throw Abort() // TODO: revert ?
        }

        stepSuccess(taskDescription)
    }

    override fun close() {
        progress.clear()
        terminal.cursor.show()
        terminal.enableLineWrap() // what if it wasn't enabled ?
    }
}

fun processSequence(terminal: Terminal, block: suspend ProcessSequenceDslContext.() -> Unit) {
    ProcessSequenceDslContext(terminal).use { ctx ->
        runBlocking { block(ctx) }
    }
}
