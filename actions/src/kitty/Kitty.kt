package com.github.hubvd.odootools.actions.kitty

import com.github.hubvd.odootools.actions.utils.WindowManager
import com.github.hubvd.odootools.workspace.Workspace
import java.lang.reflect.Proxy
import java.util.concurrent.TimeoutException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists

fun generateProxy(socketAddress: String): KittyCommands {
    val handler = KittyInvocationHandler(socketAddress)
    return Proxy.newProxyInstance(
        handler.javaClass.classLoader,
        arrayOf(KittyCommands::class.java),
        handler,
    ) as KittyCommands
}

class Kitty(
    private val windowManager: WindowManager,
    private val proxy: KittyCommands = generateProxy(
        SOCKET_ADDRESS,
    ),
) : KittyCommands by proxy {

    fun isRunning(): Boolean {
        return Path(SOCKET_ADDRESS).exists()
    }

    private fun runIfClosed(): Boolean {
        val socket = Path(SOCKET_ADDRESS)
        if (socket.notExists()) {
            val returnCode = ProcessBuilder(
                "kitty",
                "--class=$CLASS",
                "-o",
                "allow_remote_control=yes",
                "--listen-on",
                "unix:$SOCKET_ADDRESS",
                "--detach",
                "-d",
                System.getProperty("user.home"),
            ).apply {
                redirectError(ProcessBuilder.Redirect.DISCARD)
                redirectOutput(ProcessBuilder.Redirect.DISCARD)
            }.start().waitFor()

            if (returnCode != 0) {
                throw RuntimeException("kitty failed to launch")
            }

            Thread.sleep(100)

            var retries = 0
            while (true) {
                if (socket.exists()) {
                    return true
                } else if (retries < 10) {
                    Thread.sleep(10)
                    retries++
                    continue
                } else {
                    throw TimeoutException()
                }
            }
        }
        return false
    }

    fun use(block: Kitty.() -> Unit) {
        val new = runIfClosed()
        block(this@Kitty)
        if (new) closeWindow("id:1")
        focus()
    }

    fun focus() {
        windowManager.raise(CLASS)
    }

    private fun openRepo(workspace: Workspace, name: String, type: String) {
        launch(
            windowTitle = "${workspace.name} - $name",
            cwd = workspace.path.resolve(name).toString(),
            type = type,
        )
    }

    fun openGit(workspace: Workspace, odoo: Boolean = true, enterprise: Boolean = true) {
        if (!odoo && !enterprise) return
        use {
            if (odoo) openRepo(workspace, "odoo", type = "tab")
            if (enterprise) openRepo(workspace, "enterprise", type = if (odoo) "window" else "tab")
        }
    }

    companion object {
        private const val SOCKET_ADDRESS = "/tmp/kitty_odoo"
        private const val CLASS = "kitty_odoo"
    }
}
