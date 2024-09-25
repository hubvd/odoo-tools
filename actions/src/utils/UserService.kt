package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.Serializable

@Serializable
private data class Employee(
    val id: Long,
    val name: String,
    val githubLogin: String,
)

class UserService(
    private val userPersistence: UserPersistence,
    private val odooClient: OdooClient,
) {

    private fun lookup(user: User): Pair<User.OdooUser, User.GithubUser>? {
        val employee = odooClient.searchRead<Employee>("hr.employee.public", limit = 1) {
            when (val userId = user) {
                is User.GithubUser -> {
                    "github_login" eq userId.username
                }

                is User.OdooUser -> {
                    "name" like "(${userId.username})"
                }
            }
        }.firstOrNull() ?: return null
        val match = Regex("""^(.*) \(([a-z]{2,4})\)$""").find(employee.name) ?: return null
        val trigram = match.groupValues[2]
        userPersistence.save(trigram, employee.githubLogin)
        return User.OdooUser(trigram) to User.GithubUser(employee.githubLogin)
    }

    operator fun get(id: User.OdooUser): User.GithubUser? = userPersistence[id] ?: lookup(id)?.second

    operator fun get(id: User.GithubUser): User.OdooUser? = userPersistence[id] ?: lookup(id)?.first
}
