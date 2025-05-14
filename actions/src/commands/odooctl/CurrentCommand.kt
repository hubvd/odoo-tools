package com.github.hubvd.odootools.actions.commands.odooctl

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.enum
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.workspace.Workspaces

enum class InstanceProperty { Port, Url, Pid, Workspace, Database }

class CurrentCommand(private val odooctl: Odooctl, private val workspaces: Workspaces) : CliktCommand() {
    private val property by argument().enum<InstanceProperty>().default(InstanceProperty.Pid)

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()
        val instance = odooctl.instances().find { it.workspace == workspace } ?: throw Abort()
        val value = when (property) {
            InstanceProperty.Port -> instance.port
            InstanceProperty.Url -> instance.baseUrl
            InstanceProperty.Pid -> instance.pid
            InstanceProperty.Workspace -> instance.workspace.name
            InstanceProperty.Database -> instance.database
        }.toString()
        terminal.println(value)
    }
}
