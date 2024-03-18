package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.hubvd.odootools.actions.git.GitBranchType
import com.github.hubvd.odootools.actions.git.GitStatusFlags
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.workspace.Workspaces
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.io.path.div

class TestCommand(override val di: DI) : CliktCommand(), DIAware {
    override fun run() {
        println(GitStatusFlags.GIT_STATUS_OPT_INCLUDE_UNTRACKED.ordinal)

        val bits = arrayOf(
            GitStatusFlags.GIT_STATUS_OPT_INCLUDE_UNTRACKED,
            GitStatusFlags.GIT_STATUS_OPT_INCLUDE_IGNORED
        ).map { 1 shl it.ordinal }.reduce { acc, gitStatusFlags ->
            acc or gitStatusFlags
        }

        println(bits)
        return

        val workspaces by instance<Workspaces>()
        val repo = Repository.open(workspaces.list().find { it.name == "15.0" }!!.path / "odoo")

        val babar = repo.findBranch("15.0-opw-3790144-huvw", GitBranchType.LOCAL)
        repo.checkoutBranch(babar!!)
        return
    }
}
