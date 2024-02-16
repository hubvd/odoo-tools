package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.hubvd.odootools.actions.utils.Kitty
import com.github.hubvd.odootools.workspace.Workspaces
import kotlinx.coroutines.runBlocking

class OpenGitCommand(override val workspaces: Workspaces) : BasePycharmAction() {
    override fun run() = runBlocking { Kitty.openGit(workspace) }
}
