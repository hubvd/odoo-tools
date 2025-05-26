package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.currentRepository
import com.github.hubvd.odootools.actions.utils.currentRepositoryPath
import com.github.hubvd.odootools.workspace.Workspaces
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class DoneCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val amend by option("--amend").flag()

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()
        val repoPath = workspace.currentRepositoryPath() ?: throw Abort()
        val repo = workspace.currentRepository() ?: throw Abort()
        val currentBranch = repo.head().takeIf { it.isBranch() }?.branchName()
        val taskRe = Regex("""(task|opw|sentry|runbot)-(\d+)""")
        val stagedFiles = stagedFiles() + previousCommitFiles()
        val modules = stagedFiles.mapNotNull {
            when (repoPath.name) {
                "odoo" -> {
                    val parts = it.asSequence().take(3).map { it.toString() }.toList()
                    if (parts.getOrNull(0) == "addons") {
                        parts.getOrNull(1)
                    } else if (parts.getOrNull(0) == "odoo" && parts.getOrNull(1) == "addons") {
                        parts.getOrNull(2)
                    } else {
                        null
                    }
                }

                else -> {
                    it.first().toString()
                }
            }
        }.toSortedSet()
        val match = currentBranch?.let { taskRe.find(it) }

        val message = if (amend) amendedMessage(modules) else newMessage(modules, match)

        val template = createTempFile()
        template.writeText(message)

        val flags = if (amend) arrayOf("--amend") else emptyArray()
        val process = ProcessBuilder("git", "commit", *flags, "-eF", "$template")
            .inheritIO()
            .start()

        val result = process.waitFor()
        template.deleteExisting()
        if (result != 0) {
            throw ProgramResult(result)
        }
    }

    private fun amendedMessage(modules: SortedSet<String>): String {
        val oldMessage = previousCommitMessage().lines()
        val match = Regex("""(?<tags>(?:\[\w+])+)\s?(?<modules>[\w, ]+):(?<title>.*)""").matchEntire(oldMessage.first())
        return if (match == null) {
            buildList(oldMessage.size + 1) {
                modules.joinToString(", ", prefix = "[FIX] ", postfix = ":\n\n")
                addAll(oldMessage)
            }
        } else {
            buildList(oldMessage.size) {
                add(
                    modules.joinToString(
                        ", ",
                        prefix = match.groups["tags"]!!.value + " ",
                        postfix = ":" + match.groups["title"]!!.value + "\n",
                    ),
                )
                addAll(oldMessage.subList(1, oldMessage.size))
            }
        }.joinToString("\n")
    }

    private fun newMessage(modules: SortedSet<String>, match: MatchResult?): String {
        val message = buildString {
            append("[FIX] ")
            modules.joinTo(this, ", ", postfix = ":\n\n")
            arrayOf(
                "Steps to reproduce",
                "Cause of the issue",
                "Solution",
            ).forEach {
                append(it)
                appendLine()
                repeat(it.length) {
                    append('=')
                }
                append("\n\n")
            }
            if (match != null) {
                val (_, type, id) = match.groupValues
                append(type)
                append('-')
                append(id)
            }
        }
        return message
    }

    private fun previousCommitMessage(): String {
        val process = ProcessBuilder("git", "show", "-s", "--format=%B", "@")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val text = process.inputStream.bufferedReader().use {
            it.readText()
        }

        val result = process.waitFor()
        return if (result != 0) {
            throw ProgramResult(result)
        } else {
            text
        }
    }

    private fun previousCommitFiles(): List<Path> {
        if (!amend) return emptyList()

        val process = ProcessBuilder("git", "diff-tree", "--no-commit-id", "--name-only", "-r", "@")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val lines = process.inputStream.bufferedReader().use {
            it.lines().toList()
        }

        val result = process.waitFor()
        return if (result != 0) {
            emptyList()
        } else {
            lines.map { Path(it) }
        }
    }

    private fun stagedFiles(): List<Path> {
        val process = ProcessBuilder("git", "status", "--porcelain")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val lines = process.inputStream.bufferedReader().use {
            it.lines().toList()
        }

        val result = process.waitFor()
        return if (result != 0) {
            emptyList()
        } else {
            val re = Regex("""^[AMRDC]+\s+(.*)""")
            lines.mapNotNull { re.find(it)?.groups?.get(1)?.value?.let { Path(it) } }
        }
    }
}
