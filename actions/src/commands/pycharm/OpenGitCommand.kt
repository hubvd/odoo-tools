package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.hubvd.odootools.actions.utils.Sway
import com.github.hubvd.odootools.workspace.Workspaces
import kotlinx.coroutines.runBlocking

class OpenGitCommand(override val workspaces: Workspaces) : BasePycharmAction() {
    override fun run() = runBlocking { Sway.openGit(workspace) }
}
