package com.github.hubvd.odootools.actions.utils

import kotlin.io.path.*

class DbManager {
    fun list(includeSavepoints: Boolean = true): List<String>? {
        val process = ProcessBuilder(
            "psql",
            "-Atc",
            "select datname from pg_database where datistemplate = false order by datname",
            "postgres",
        )
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val lines = process.inputStream.bufferedReader().use {
            it.lines().toList()
        }

        val result = process.waitFor()
        if (result != 0) return null
        return if (includeSavepoints) {
            lines
        } else {
            lines.filterNot { it.endsWith("__SAVEPOINT") }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun copy(from: String, to: String) {
        val existingDatabases = list()!!
        if (!existingDatabases.contains(from)) {
            throw IllegalArgumentException("database $from does not exist")
        }
        if (existingDatabases.contains(to) && !delete(to)) {
            throw RuntimeException("Failed to delete database $to")
        }
        val process = ProcessBuilder("createdb", "-T", from, to)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        val fileStoreTargetRoot = Path(System.getProperty("user.home")) / ".local/share/Odoo/filestore"
        val fromFileStore = fileStoreTargetRoot / from
        val toFileStore = fileStoreTargetRoot / to
        toFileStore.deleteRecursively()

        if (fromFileStore.exists()) {
            toFileStore.createParentDirectories()
            fromFileStore.copyToRecursively(toFileStore, followLinks = false)
        }

        process.waitFor()
    }

    fun delete(name: String): Boolean {
        return ProcessBuilder("dropdb", name)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    }
}
