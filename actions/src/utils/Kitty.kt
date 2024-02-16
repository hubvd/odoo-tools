package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.workspace.Workspace
import com.github.pgreze.process.Redirect.SILENT
import com.github.pgreze.process.process
import kotlinx.coroutines.delay
import kotlin.io.path.Path
import kotlin.io.path.notExists

object Kitty {

    private const val SOCKET_ADDRESS = "/tmp/kitty_odoo"
    suspend fun runIfClosed() {
        if (Path(SOCKET_ADDRESS).notExists()) {
            process(
                "kitty",
                "--class=kitty_odoo",
                "-o",
                "allow_remote_control=yes",
                "--listen-on",
                "unix:$SOCKET_ADDRESS",
                "--detach",
            )
            delay(100)
        }
    }

    suspend fun launch(
        vararg command: String,
        hold: Boolean = false,
        title: String? = null,
        type: String? = null,
        cwd: String? = null,
    ) {
        val args = buildList {
            addAll(
                arrayOf(
                    "kitty",
                    "@",
                    "--to",
                    "unix:$SOCKET_ADDRESS",
                    "launch",
                )
            )
            if (hold) add("--hold")
            title?.let {
                add("--title")
                add(it)
            }
            type?.let {
                add("--type")
                add(it)
            }
            cwd?.let {
                add("--cwd")
                add(it)
            }
            addAll(command)
        }
        process(*args.toTypedArray())
    }

    private suspend fun openRepo(workspace: Workspace, name: String) {
        launch(
            "fish",
            title = "odoo - ${workspace.name}",
            cwd = workspace.path.resolve(name).toString(),
            type = "tab",
        )
    }

    suspend fun openGit(workspace: Workspace, odoo: Boolean = true, enterprise: Boolean = true) {
        if (!odoo && !enterprise) return
        runIfClosed()
        if (odoo) openRepo(workspace, "odoo")
        if (enterprise) openRepo(workspace, "enterprise")
    }
}
