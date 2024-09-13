package com.github.hubvd.odootools.actions.utils

import com.github.pgreze.process.InputSource
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking

class Pycharm {

    private val pycharmBin = System.getProperty("user.home") + "/.local/share/JetBrains/Toolbox/scripts/pycharm"

    fun open(path: String, line: Int? = null, column: Int? = null, blocking: Boolean = false) {
        val cmd = buildList {
            add(pycharmBin)
            line?.let {
                add("--line")
                add(line.toString())
            }
            column?.let {
                add("--column")
                add(column.toString())
            }
            add(path)
        }

        ProcessBuilder(*cmd.toTypedArray())
            .apply {
                redirectOutput(ProcessBuilder.Redirect.DISCARD)
                redirectError(ProcessBuilder.Redirect.DISCARD)
            }.start().takeIf { blocking }?.waitFor()
    }
}

fun <T> menu(
    choices: List<T>,
    lines: Int? = choices.size,
    prompt: String? = null,
    transform: (T) -> String = { it.toString() },
): T? = runBlocking {
    if (choices.isEmpty()) return@runBlocking null
    if (choices.size == 1) return@runBlocking choices.first()

    val map = choices.associateBy(transform)

    val (code, output) = process(
        *buildList {
            add("bemenu")
            add("-l")
            add(lines.toString())
            prompt?.let {
                add("-p")
                add(it)
            }
        }.toTypedArray(),
        stdin = InputSource.FromStream { out ->
            out.bufferedWriter().use { buff ->
                map.keys.forEach {
                    buff.write(it)
                    buff.newLine()
                }
            }
        },
        stdout = Redirect.CAPTURE,
        stderr = Redirect.SILENT,
    )
    if (code == 0) output.firstOrNull()?.trim()?.let { map[it] } else null
}

fun selectInstance(instances: List<OdooInstance>): OdooInstance? = menu(instances) {
    buildString {
        append(it.workspace.name)
        if (it.database != it.workspace.name) {
            append(" (")
            append(it.database)
            append(')')
        }
        append(" [")
        append(it.port)
        append(']')
    }
}
