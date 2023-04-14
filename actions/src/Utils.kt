package com.github.hubvd.odootools.actions

import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.InputSource
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.readText

data class OdooInstance(
    val pid: Int,
    val port: Int,
    val database: String,
    val workspace: Workspace,
) {
    val baseUrl: String
        get() {
            // Use unique host in order to not have shared cookies
            val host = "127.0.0." + workspace.version.toString().replace(".", "")
            @Suppress("HttpUrlsUsage")
            return "http://$host:$port"
        }
}

class Odooctl(private val workspaces: Workspaces) {
    fun instances(): List<OdooInstance> = runBlocking {
        val workspaceList = workspaces.list()
        process("ps", "a", "-o", "pid,args", stdout = Redirect.CAPTURE, stderr = Redirect.SILENT)
            .output
            .asSequence()
            .mapNotNull { ODOO_PROC_RE.find(it)?.groups?.get(1)?.value }
            .map { pid -> pid to Path("/proc/$pid/cmdline").readText().split('\u0000') }
            .filter { !it.second.contains("shell") }
            .mapNotNull { (pid, cmdline) ->
                val db = cmdline.find { it.startsWith("--database=") }?.removePrefix("--database=")
                    ?: return@mapNotNull null

                val port = cmdline.find { it.startsWith("--http-port=") }?.removePrefix("--http-port=")
                    ?.toInt()
                    ?: 8069

                OdooInstance(
                    pid = pid.toInt(),
                    port = port,
                    db,
                    workspaceList.first { it.path == Path("/proc/$pid/cwd").toRealPath() },
                )
            }.toList()
    }

    fun killAll() {
        runBlocking {
            instances().takeIf { it.isNotEmpty() }?.run {
                process("kill", *map { it.pid.toString() }.toTypedArray())
            }
        }
    }

    companion object {
        val ODOO_PROC_RE = """^\s+(\d+) \S*python .*odoo""".toRegex()
    }
}

class Pycharm {

    private val pycharmBin = System.getProperty("user.home") + "/.local/share/JetBrains/Toolbox/scripts/pycharm"

    fun open(path: String, line: Int? = null, column: Int? = null) {
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

        runBlocking {
            process(
                *cmd.toTypedArray(),
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            )
        }
    }
}

fun <T> menu(
    choices: List<T>,
    lines: Int? = choices.size,
    transform: (T) -> String = { it.toString() },
): T? = runBlocking {
    if (choices.isEmpty()) return@runBlocking null
    if (choices.size == 1) return@runBlocking choices.first()

    val map = choices.associateBy(transform)

    val (code, output) = process(
        "bemenu",
        "-l",
        lines.toString(),
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
