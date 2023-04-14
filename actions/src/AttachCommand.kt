package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.exitProcess

class AttachCommand(private val odooctl: Odooctl) : CliktCommand() {
    override fun run() {
        val instances = odooctl.instances()
        if (instances.isEmpty()) exitProcess(1)
        val choice = menu(instances) { it.workspace.name } ?: exitProcess(1)

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder(URI.create(choice.baseUrl + "/debug/attach")).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            println(response.body())
            exitProcess(1)
        }
    }
}
