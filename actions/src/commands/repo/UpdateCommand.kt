package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.*

class UpdateCommand : CliktCommand() {
    private val workspaceRepositories by requireObject<List<WorkspaceRepositories>>()

    override fun run() = runBlocking {
        val onBaseResults = workspaceRepositories.map { repos ->
            async(repos.dispatcher) {
                val base = repos.workspace.base
                val onBase = sequenceOf(
                    repos.odoo.await(),
                    repos.enterprise.await(),
                    repos.designThemes.await(),
                ).none {
                    it.head().takeIf { it.isBranch() }?.branchName() != base
                }
                repos to onBase
            }
        }.awaitAll()
        val onBase = onBaseResults.filter { it.second }.map { it.first }
        val skipped = onBaseResults.filterNot { it.second }.map { it.first }

        val cleanResults = onBase.map { repos ->
            async(repos.dispatcher) {
                arrayOf(repos.odoo, repos.enterprise, repos.designThemes).all {
                    val repo = it.await()
                    (repo.status().count() == 0L)
                }
            }
        }.awaitAll()
        val clean = onBase.filterIndexed { index, _ -> cleanResults[index] }
        val dirty = onBase.filterIndexed { index, _ -> !cleanResults[index] }

        println(onBase.map { it.workspace.name })
        println(skipped.map { it.workspace.name })
        println("clean:" + clean.map { it.workspace.name })
        println("dirty:" + dirty.map { it.workspace.name })

        clean.map { repos ->
            launch(repos.dispatcher) {
                arrayOf(repos.odoo, repos.enterprise, repos.designThemes).forEach {
                    val repo = it.await()
                    val upstream = repo.head().upstream()?.target()!!
                    val commitsBehind = repo.head().target()!!.aheadBehind(upstream).second
                    if (commitsBehind == 0L) return@forEach
                    repo.checkoutTree(repo.head().upstream()!!)
                    repo.head().setTarget(repo.head().upstream()!!.target()!!)
                    println(repo.path)
                }
            }
        }.joinAll()
    }
}
