package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.hubvd.odootools.workspace.Workspaces
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.io.path.Path

/**
 * This is a base class to configured as an external tool in Pycharm
 * https://www.jetbrains.com/help/idea/configuring-third-party-tools.html
 * The first argument is the name of the subcommand followed by a few
 * Built-in IDE macros. For example if we create a command named example,
 * the following should be put in the Arguments input:
 * example "$ProjectFileDir$" "$FilePath$" "$LineNumber$" "$ColumnNumber$" $SelectedText$
 */
abstract class PycharmActionCommand(name: String? = null) : CliktCommand(name = name), DIAware {
    private val workspaces by instance<Workspaces>()

    protected val workspace by argument().convert { option -> workspaces.list().first { it.path == Path(option) } }
    protected val file by argument()
    protected val line by argument()
    protected val column by argument()
    protected val selection by argument().multiple().transformAll { it.joinToString(" ") }
}
