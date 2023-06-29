package com.github.hubvd.odootools.actions

import kotlinx.coroutines.runBlocking
import org.kodein.di.DI

class OpenGitCommand(override val di: DI) : PycharmActionCommand() {
    override fun run() = runBlocking { Sway.openGit(workspace) }
}
