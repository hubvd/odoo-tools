package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.hubvd.odootools.actions.utils.User
import com.github.hubvd.odootools.actions.utils.userIdOption
import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.Serializable

@Serializable
private data class Employee(
    val id: Long,
    val name: String,
    val githubLogin: String,
    val departmentId: ModelReference,
)

class WhoIsCommand(private val odooClient: OdooClient) : CliktCommand() {
    private val userId by userIdOption().required()

    override fun run() {
        val user = odooClient.searchRead<Employee>("hr.employee.public", limit = 1) {
            when (val userId = userId) {
                is User.GithubUser -> {
                    "github_login" eq userId.username
                }

                is User.OdooUser -> {
                    "name" like "(${userId.username})"
                }
            }
        }.firstOrNull() ?: throw ProgramResult(1)
        terminal.println(
            buildString {
                val match = Regex("""^(.*) \(([a-z]{2,4})\)$""").find(user.name)
                if (match != null) {
                    val (name, trigram) = match.groupValues.drop(1)
                    append(magenta(name))
                    append(' ')
                    append(trigram)
                } else {
                    append(magenta(user.name))
                }
                appendLine()
                append(cyan("github"))
                append(' ')
                append(user.githubLogin)
                appendLine()
                append(cyan("department"))
                append(' ')
                append(user.departmentId.name)
            },
        )
    }
}
