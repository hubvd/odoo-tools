package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.hubvd.odootools.actions.ActionsConfig
import com.github.hubvd.odootools.actions.git.GitBranchType
import com.github.hubvd.odootools.actions.git.GitStatusFlags
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.workspace.Workspaces
import kotlinx.coroutines.internal.resumeCancellableWith
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.io.path.div

class TestCommand(override val di: DI) : CliktCommand(), DIAware {
    override fun run() {
        val conf by instance<ActionsConfig>()

        fun menu(line: Int): List<String> {
            return conf.dmenu.map {
                it.replace("{{lines}}", line.toString())
            }

        }
        println(menu(2))



    }
}
