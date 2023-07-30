package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.streams.asSequence

private data class Interface(val name: String, val address: Inet4Address)
class QrCommand(
    private val terminal: Terminal,
    private val odooctl: Odooctl,
    private val workspaces: Workspaces,
) : CliktCommand(
    help = "Show a QR code pointing to running odoo instances",
) {
    override fun run() {
        val interfaces = NetworkInterface.networkInterfaces()
            .asSequence()
            .filterNot { it.interfaceAddresses.any { it.address.isLoopbackAddress } }
            .mapNotNull {
                Interface(it.name, it.interfaceAddresses.map { it.address }.filterIsInstance<Inet4Address>().first())
            }
            .toList()

        val currentWorkspace = workspaces.current()

        val rows = odooctl.instances()
            .run { if (currentWorkspace != null) filter { it.workspace == currentWorkspace } else this }
            .sortedBy { it.workspace.version }
            .map { instance -> instance to interfaces.map { InterfaceWidget(it, instance.port) } }

        terminal.println(
            grid {
                align = TextAlign.CENTER
                rows.forEach { row ->
                    row {
                        cell(row.first.workspace.name) {
                            columnSpan = 2
                            style = TextStyle(TextColors.brightBlue, bold = true)
                        }
                    }
                    row {
                        cellsFrom(row.second)
                        padding { bottom = 1 }
                    }
                }
            },
        )
    }
}

private class InterfaceWidget(private val `interface`: Interface, port: Int) : Widget {

    private val url = "http://${`interface`.address.hostAddress}:$port/web"

    private val qr = runBlocking {
        process("qrencode", "-t", "utf8", url, stdout = Redirect.CAPTURE)
    }.output.map {
        it.map { if (it.isWhitespace()) Span.space() else Span.word(it.toString()) }
    }

    override fun measure(t: Terminal, width: Int) = qr.first().size.run { WidthRange(this, this) }

    override fun render(t: Terminal, width: Int) = Lines(
        buildList(qr.size + 2) {
            add(Line(listOf(Span.word(`interface`.name))))
            add(Line(listOf(Span.word(url))))
            qr.forEach {
                add(Line(it))
            }
        },
    )
}
