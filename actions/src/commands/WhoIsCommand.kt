package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.hubvd.odootools.actions.utils.Employee
import com.github.hubvd.odootools.actions.utils.EmployeeService
import com.github.hubvd.odootools.actions.utils.userIdOption
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import kotlinx.serialization.Serializable

@Serializable
private class EmployeeWithDepartment(
    override val id: Long,
    override val name: String,
    override val githubLogin: String,
    val departmentId: ModelReference,
) : Employee

class WhoIsCommand(private val employeeService: EmployeeService) : CliktCommand() {
    private val userId by userIdOption().required()

    override fun run() {
        val employee = employeeService.find<EmployeeWithDepartment>(userId) ?: throw Abort()
        terminal.println(
            buildString {
                append(magenta(employee.fullName))
                append(' ')
                append(employee.trigram)
                appendLine()
                append(cyan("github"))
                append(' ')
                append(employee.githubLogin)
                appendLine()
                append(cyan("department"))
                append(' ')
                append(employee.departmentId.name)
            },
        )
    }
}
