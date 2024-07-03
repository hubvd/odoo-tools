package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces
import com.sun.security.auth.module.UnixSystem
import java.nio.file.NoSuchFileException
import java.util.concurrent.CompletableFuture
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

class Odooctl(private val workspaces: Workspaces) {

    private fun findHttpPort(pid: Long): Int {
        val sockets = Path("/proc/$pid/fd").listDirectoryEntries().mapNotNull {
            if (it.name.toInt() < 3) return@mapNotNull null
            try {
                val link = it.readSymbolicLink().toString()
                if (!link.startsWith("socket:[")) {
                    return@mapNotNull null
                }
                // Let's assume that a socket:[*] name means it is a socket...
                link.substring(8..link.length - 2)
            } catch (e: NoSuchFileException) {
                return@mapNotNull null
            }
        }

        Path("/proc/$pid/net/tcp").useLines { lines ->
            for (line in lines) {
                val columns = line.trim().split(WHITESPACE_REGEX)
                if (
                    columns.size == 17 && // Check that we skipped the header
                    columns[3] == "0A" && // Check if state is LISTEN
                    columns[9] in sockets // Check that the inode matches an owned socket
                ) {
                    val localAddress = columns[1]
                    val portHex = localAddress.split(":")[1]
                    return portHex.toInt(16)
                }
            }
        }

        return -1
    }

    fun instances(): List<OdooInstance> {
        val workspaceList = workspaces.list()
        val uid = UnixSystem().uid

        val instances = mutableListOf<OdooInstance>()
        val pids = mutableListOf<Long>()

        for (processHandle in ProcessHandle.allProcesses()) {
            if (processHandle.parent().map { it.pid() }.getOrElse { -1L } in pids) {
                continue // Skip workers
            }

            val handle = LinuxProcessHandle(processHandle)
            if (handle.loginuid() != uid) {
                continue
            }

            val info = handle.info()
            if (info.command().getOrNull()?.contains("/python") != true) {
                continue
            }
            val arguments = info.arguments()
            if (arguments.isEmpty || !arguments.get().any { it.contains("odoo") }) {
                continue
            }
            if (arguments.get().contains("shell")) {
                continue
            }

            val db = arguments.get().find { it.startsWith("--database=") }?.removePrefix("--database=") ?: continue
            val pid = handle.pid()
            val port = findHttpPort(pid)
            instances.add(
                OdooInstance(
                    pid,
                    port,
                    db,
                    workspaceList.first { it.path == handle.cwd() },
                ),
            )
            pids.add(pid)
        }

        return instances.sortedWith(compareBy({ it.workspace.version }, { it.database }))
    }

    fun kill(instance: OdooInstance): CompletableFuture<ProcessHandle>? {
        ProcessHandle.of(instance.pid).getOrNull()?.run {
            destroy()
            return onExit()
        }
        return null
    }

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}

data class OdooInstance(
    val pid: Long,
    val port: Int,
    val database: String,
    val workspace: Workspace,
) {

    // Use unique host in order to avoid shared cookies
    val baseUrl: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        @Suppress("HttpUrlsUsage")
        "http://$database.localhost:$port"
    }
}
