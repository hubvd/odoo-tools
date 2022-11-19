package com.github.hubvd.odootools.worktree

import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.terminal.Terminal
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.github.pgreze.process.unwrap
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

class ProcessSequenceDslContext(private val terminal: Terminal) : AutoCloseable {
    private var cwd: File = File(System.getProperty("user.dir"))
    private val progress = terminal.textAnimation<String> { message -> (blue + bold)(message) }

    init {
        terminal.cursor.hide(showOnExit = true)
    }

    fun cd(path: Path) {
        cwd = path.toFile()
    }

    suspend fun capture(vararg cmd: String): List<String> {
        return process(*cmd, stdout = Redirect.CAPTURE, stderr = Redirect.CAPTURE).unwrap()
    }

    suspend fun run(vararg cmd: String) {
        val description = Path(cmd[0]).name + cmd.drop(1).joinToString(" ", prefix = " ")
        terminal.println((blue + underline)(description))
        progress.update("$description...")
        process(*cmd, directory = cwd, stdout = Redirect.CAPTURE, stderr = Redirect.CAPTURE, consumer = { line ->
            terminal.println(line)
        }).unwrap()
    }

    override fun close() {
        progress.clear()
        terminal.cursor.show()
    }
}

fun processSequence(terminal: Terminal, block: suspend ProcessSequenceDslContext.() -> Unit) {
    ProcessSequenceDslContext(terminal).use { ctx ->
        runBlocking { block(ctx) }
    }
}
