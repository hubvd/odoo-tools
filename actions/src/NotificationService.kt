package com.github.hubvd.odootools.actions

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking

interface NotificationService {
    fun info(title: String, message: String? = null)
    fun warn(title: String, message: String? = null)
    fun success(title: String, message: String? = null)
}

class TerminalNotificationService(private val terminal: Terminal) : NotificationService {

    private fun buildMsg(title: String, message: String?) = buildString {
        if (message != null) {
            append(bold(title))
            append(' ')
            append(message)
        } else {
            append(title)
        }
    }

    override fun info(title: String, message: String?) {
        terminal.println(buildMsg(title, message))
    }

    override fun warn(title: String, message: String?) {
        terminal.println(red(buildMsg(title, message)))
    }

    override fun success(title: String, message: String?) {
        terminal.println(green(buildMsg(title, message)))
    }
}

class SystemNotificationService : NotificationService {
    override fun info(title: String, message: String?) {
        runBlocking {
            process("notify-send", title, message ?: "")
        }
    }

    override fun warn(title: String, message: String?) {
        runBlocking {
            process("notify-send", "-h", "string:frcolor:#FF0000", title, message ?: "")
        }
    }

    override fun success(title: String, message: String?) {
        runBlocking {
            process("notify-send", "-h", "string:frcolor:#00FF00", title, message ?: "")
        }
    }
}
