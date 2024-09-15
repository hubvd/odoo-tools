package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.hubvd.odootools.odoo.TestTag
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*

enum class CompletionType { Addon, Qunit, Hoot, TestTag, Lang }

@Serializable
private data class Ctag(val name: String, val scope: String? = null, val scopeKind: String? = null)

class CompleteCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val token by option(help = "current token").required()
    private val type by option().enum<CompletionType>().required()

    override val hiddenFromHelp = true

    private lateinit var arguments: List<String>
    private lateinit var workspace: Workspace

    override fun run() {
        arguments = if (currentContext.terminal.terminalInfo.inputInteractive) {
            emptyList()
        } else {
            System.`in`.bufferedReader().use { it.lineSequence().toList() }
        }

        workspace = workspaces.current() ?: workspaces.default()

        when (type) {
            CompletionType.Addon -> completeAddons()
            CompletionType.Qunit -> completeQUnitTests()
            CompletionType.Hoot -> completeHootTests()
            CompletionType.TestTag -> completeTestTags()
            CompletionType.Lang -> completeLang()
        }.forEach { println(it) }
    }

    private fun completeLang(): Sequence<String> = sequence {
        val langPath = workspace.path / "odoo/odoo/addons/base/data/res.lang.csv"
        try {
            langPath.forEachLine { line ->
                if (line.startsWith(""""id","name","code"""")) return@forEachLine
                val firstComma = line.indexOf(',')
                if (firstComma == -1) return@forEachLine
                val secondComma = line.indexOf(',', firstComma + 1)
                if (secondComma == -1) return@forEachLine
                val thirdComma = line.indexOf(',', secondComma + 1)
                if (thirdComma == -1) return@forEachLine
                val value = line.substring(secondComma + 2, thirdComma - 1)
                val description = line.substring(firstComma + 2, secondComma - 1)
                yield("$value\t$description")
            }
        } catch (_: IOException) { }
    }

    private fun completeTestTags() = sequence {
        val tags = token.split(',')
        val lastTag = tags.lastOrNull()
        val previousTags = if (tags.size > 1) tags.dropLast(1).joinToString(",", postfix = ",") else ""
        val tag = lastTag?.let { TestTag(it) }

        val addons = findAddons().toList()

        var addonPath: Path? = null
        if (tag?.module != null) {
            addons.find { it.name == tag.module }.let { addonPath = it }
        }

        if (tag?.clazz == null && tag?.method == null) {
            addons.forEach { yield(previousTags + "/" + it.name) }
        } else if (tag.module != null && tag.method == null && addonPath != null) {
            ctags(addonPath!! / "tests", "c").forEach {
                yield("$previousTags${tag.prefix ?: ""}/${tag.module}:${it.name}")
            }
        } else if (tag.module != null && addonPath != null) {
            if (tag.clazz == null) {
                ctags(addonPath!! / "tests", "f").filter { it.name.startsWith("test_") }
                    .forEach { yield("$previousTags${tag.prefix ?: ""}/${tag.module}.${it.name}") }
            } else {
                ctags(addonPath!! / "tests", "cfm")
                    .filter { (name, scope, scopeKind) ->
                        name.startsWith("test_") && scopeKind == "class" && scope == tag.clazz
                    }
                    .forEach { yield("$previousTags${tag.prefix ?: ""}/${tag.module}:${tag.clazz}.${it.name}") }
            }
        }
    }

    private fun ctags(path: Path, symbols: String): List<Ctag> = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        process(
            "ctags",
            "--kinds-Python=$symbols",
            "--recurse",
            "--output-format=json",
            "$path",
            stdout = Redirect.CAPTURE,
            stderr = Redirect.SILENT,
        )
            .output
            .map { json.decodeFromString(Ctag.serializer(), it) }
    }

    private fun completeQUnitTests() = runBlocking {
        @Suppress("ktlint:standard:max-line-length")
        val regex = """(?<=QUnit\.module\(|QUnit\.test\(|QUnit\.debug\(|QUnit\.only\()(?P<quote>["'`])(?P<name>.+)(?=(?P=quote))"""
        process(
            "rg",
            "--no-ignore",
            "--no-filename",
            "--pcre2",
            "--type=js",
            "--only-matching",
            "--replace=\$name",
            regex,
            "${workspace.path / "odoo/addons/web/static/tests"}",
            stdout = Redirect.CAPTURE,
            stderr = Redirect.SILENT,
        ).output.asSequence()
    }

    private fun completeHootTests() = runBlocking {
        @Suppress("ktlint:standard:max-line-length")
        val regex = """test(?:\.(?:debug|skip|only|todo)|\.(?:tags|timeout)\(?["'`\w]+\)?)?\(\s*(?P<quote>["'`])(?P<name>.+)(?=(?P=quote))"""
        process(
            "rg",
            "--multiline",
            "--no-ignore",
            "--no-filename",
            "--pcre2",
            "--glob=*.test.js",
            "--only-matching",
            "--replace=\$name",
            regex,
            "${workspace.path}",
            stdout = Redirect.CAPTURE,
            stderr = Redirect.SILENT,
        ).output.asSequence()
    }

    private fun completeAddons() = sequence {
        val addons = findAddons().map { it.name }.toList()
        val currents = token.split(',').run { if (lastOrNull() !in addons) dropLast(1) else this }
        val prefix = if (currents.isEmpty()) "" else currents.joinToString(",", postfix = ",")
        addons.filter { it !in currents }.forEach { yield(prefix + it) }
    }

    private fun findAddons() = sequence {
        yield("odoo/addons")
        yield("odoo/odoo/addons")
        if ("--community" !in arguments) yield("enterprise")
    }
        .map { workspace.path / it }
        .flatMap { it.listDirectoryEntries() }
        .filter { (it / "__manifest__.py").exists() }
}
