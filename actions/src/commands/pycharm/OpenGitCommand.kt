package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.workspace.Workspaces

class OpenGitCommand(override val workspaces: Workspaces, private val kitty: Kitty) : BasePycharmAction() {
    override fun run() = kitty.openGit(workspace)
}
