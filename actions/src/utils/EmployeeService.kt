package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.core.domain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

private val trigramRe = Regex("""^(.*?) ?\(([a-zA-Z]{2,5})\)$""")

interface Employee {
    val id: Long
    val name: String
    val githubLogin: String

    val trigram: String
        get() = trigramRe.find(name)?.groupValues?.get(2)?.lowercase() ?: error("Couldn't extract trigram")

    val fullName: String
        get() = trigramRe.find(name)?.groupValues?.get(1) ?: error("Couldn't extract trigram")
}

@Serializable
data class BaseEmployee(
    override val id: Long,
    override val name: String,
    override val githubLogin: String,
) : Employee

class EmployeeService(
    private val userPersistence: UserPersistence,
    private val odooClient: OdooClient,
) {

    inline fun <reified T : Employee> find(user: User): T? = find(user, serializer<T>())

    fun <T : Employee> find(user: User, deserializer: KSerializer<T>): T? {
        val domain = domain {
            when (val userId = user) {
                is User.GithubUser -> {
                    "github_login" eq userId.username
                }

                is User.OdooUser -> {
                    "name" `=ilike` "%(${userId.username})"
                }
            }
        }
        val employee = odooClient
            .searchRead("hr.employee.public", deserializer = deserializer, limit = 1, domain = domain)
            .firstOrNull()
            ?: return null

        userPersistence.save(employee.trigram, employee.githubLogin)
        return employee
    }

    operator fun get(id: User.OdooUser): User.GithubUser? =
        userPersistence[id] ?: find<BaseEmployee>(id)?.let { User.GithubUser(it.githubLogin) }

    operator fun get(id: User.GithubUser): User.OdooUser? =
        userPersistence[id] ?: find<BaseEmployee>(id)?.let { User.OdooUser(it.trigram) }
}
