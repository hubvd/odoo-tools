package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

class AttachCommand(
    private val odooctl: Odooctl,
    private val httpHandler: HttpHandler,
) : CliktCommand() {
    override fun run() {
        val instances = odooctl.instances()
        if (instances.isEmpty()) throw CliktError("No instances running")
        val choice = menu(instances) { it.workspace.name } ?: throw Abort()

        val request = Request(Method.GET, choice.baseUrl + "/debug/attach")
        val response = httpHandler(request)

        if (!response.status.successful) {
            throw CliktError("Couldn't attach debugger " + response.bodyString())
        }
    }
}
