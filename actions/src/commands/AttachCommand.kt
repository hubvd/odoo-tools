package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.actions.utils.selectInstance
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

class AttachCommand(
    private val odooctl: Odooctl,
    private val httpHandler: HttpHandler,
) : CliktCommand() {
    override fun help(context: Context) = "Attach the selected odoo instances to a remote debugger"

    override fun run() {
        val instances = odooctl.instances()
        if (instances.isEmpty()) throw CliktError("No instances running")
        val choice = selectInstance(instances) ?: throw Abort()

        val request = Request(Method.GET, choice.baseUrl + "/debug/attach")
        val response = httpHandler(request)

        when (response.status.code) {
            200 -> return
            404 -> throw CliktError("Failed to attach debugger: debug module not installed")
            else -> throw CliktError("Failed to attach debugger: ${response.bodyString()}")
        }
    }
}
