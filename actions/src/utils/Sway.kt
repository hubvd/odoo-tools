package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.workspace.Workspace
import com.github.pgreze.process.Redirect.SILENT
import com.github.pgreze.process.process
import kotlinx.coroutines.delay
import kotlin.io.path.Path
import kotlin.io.path.notExists

object Sway {

    private const val SOCKET_ADDRESS = "/tmp/mykitty"
    suspend fun runScratchpadIfClosed() {
        if (Path(SOCKET_ADDRESS).notExists()) {
            process(
                "kitty",
                "--class=kitty_scratchpad",
                "-o",
                "allow_remote_control=yes",
                "--listen-on",
                "unix:$SOCKET_ADDRESS",
                "--detach",
            )
            delay(100)
        }
    }

    suspend fun showScratchpad() {
        val scratchPad = "[app_id=\"kitty_scratchpad\"]"
        "$scratchPad move container to workspace current"()
        "$scratchPad focus"()
    }

    private suspend operator fun String.invoke() = process(
        "sway",
        this,
        stdout = SILENT,
        stderr = SILENT,
    )

    suspend fun openGit(workspace: Workspace) {
        runScratchpadIfClosed()
        process(
            "kitty",
            "@",
            "--to",
            "unix:$SOCKET_ADDRESS",
            "launch",
            "--title",
            "odoo - ${workspace.name}",
            "--type",
            "tab",
            "--cwd",
            workspace.path.resolve("odoo").toString(),
            "fish",
            stdout = SILENT,
            stderr = SILENT,
        )
        process(
            "kitty",
            "@",
            "--to",
            "unix:$SOCKET_ADDRESS",
            "launch",
            "--title",
            "enterprise - ${workspace.name}",
            "--type",
            "window",
            "--cwd",
            workspace.path.resolve("enterprise").toString(),
            "fish",
            stdout = SILENT,
            stderr = SILENT,
        )
        showScratchpad()
    }
}
