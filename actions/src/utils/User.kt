package com.github.hubvd.odootools.actions.utils

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.MutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option

fun ParameterHolder.userIdOption(): MutuallyExclusiveOptions<User, User?> = mutuallyExclusiveOptions(
    option("-o", "--odoo-username").convert { User.OdooUser(it) },
    option("-g", "--github-username").convert { User.GithubUser(it) },
).single()

sealed class User {
    data class OdooUser(val username: String) : User()
    data class GithubUser(val username: String) : User()
}
