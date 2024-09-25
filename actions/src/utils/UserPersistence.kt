package com.github.hubvd.odootools.actions.utils

import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.div

class UserPersistence {
    private val usersPath: Path
    private val users: Properties by lazy { load() }

    init {
        val dataDir = Path(System.getenv("XDG_DATA_HOME") ?: (System.getProperty("user.home") + "/.local/share"))
        usersPath = dataDir / "odoo-tools/users.properties"
    }

    private fun load(): Properties = try {
        usersPath.bufferedReader().use { reader ->
            Properties().apply { load(reader) }
        }
    } catch (_: NoSuchFileException) {
        Properties()
    }

    operator fun get(id: User.OdooUser) = users.getProperty(id.username)?.let { User.GithubUser(it) }

    operator fun get(id: User.GithubUser) =
        users.entries.find { it.value == id.username }?.let { User.OdooUser(it.key as String) }

    fun save(odooId: String, githubId: String) {
        users.setProperty(odooId, githubId)
        usersPath.bufferedWriter().use { writer ->
            users.store(writer, null)
        }
    }
}
