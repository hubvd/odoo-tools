package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
import kotlin.io.path.*

class OdooStubs(dataDir: DataDir) {

    private val rootPath = dataDir["odoo-stubs"]
    private val masterPath = rootPath / "master"

    context(ProcessSequenceDslContext)
    suspend fun create(workspace: Workspace) {
        rootPath.createDirectories()
        if (masterPath.notExists()) clone()

        val stubsVersion = when (workspace.version) {
            in 14.0..<15.0 -> "14.0"
            in 15.0..<16.0 -> "15.0"
            else -> "16.0"
        }
        val stubsPath = rootPath / stubsVersion
        if (stubsPath.notExists()) {
            cd(masterPath)
            run("git", "worktree", "add", "$stubsPath", stubsVersion)
        }

        val target = workspace.path / "odoo-stubs"
        target.deleteIfExists()
        target.createSymbolicLinkPointingTo(stubsPath)
    }

    context(ProcessSequenceDslContext)
    private suspend fun clone() {
        assert(masterPath.notExists())
        cd(rootPath)
        run(
            "git",
            "clone",
            "git@github.com:odoo-ide/odoo-stubs.git",
            "master"
        )
        cd(masterPath)
        run(
            "git",
            "checkout",
            "master"
        )
    }

}
