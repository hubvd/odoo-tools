package com.github.hubvd.odootools.actions.utils

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.MutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option

fun ParameterHolder.userIdOption(): MutuallyExclusiveOptions<UserId, UserId?> = mutuallyExclusiveOptions(
    option("-o", "--odoo-username").convert { UserId.OdooUserId(it) },
    option("-g", "--github-username").convert { UserId.GithubUserId(it) },
).single()

sealed class UserId {
    data class OdooUserId(val username: String) : UserId()
    data class GithubUserId(val username: String) : UserId()
}
