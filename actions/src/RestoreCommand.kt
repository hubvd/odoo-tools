package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.terminal.Terminal
import com.github.pgreze.process.InputSource
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.body.form
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

sealed class DumpSource {

    abstract val name: String

    data class ZipSource(val file: File) : DumpSource() {
        override val name: String
            get() = file.name.removeSuffix(".zip")
    }

    data class RemoteSource(val uri: URI) : DumpSource() {
        override val name: String
            get() = uri.host.substringBefore('.').removeSuffix("-all") + "-all"
    }
}

class RestoreCommand(
    private val terminal: Terminal,
    private val dumpPassword: Secret,
    private val httpHandler: HttpHandler,
) : CliktCommand() {
    private val source by mutuallyExclusiveOptions(
        option("-z", "--zip").file(mustExist = true, canBeFile = true, canBeDir = false)
            .convert { DumpSource.ZipSource(it) }
            .validate { it.file.name.endsWith(".zip") },
        option("-u", "--url").convert { DumpSource.RemoteSource(URI.create(it)) },
    ).single().required()

    private val name by argument().defaultLazy { source.name }

    override fun run() {
        val stream = when (val source = source) {
            is DumpSource.ZipSource -> FileInputStream(source.file)
            is DumpSource.RemoteSource -> download(source)
        }

        unzip(stream)
    }

    private fun download(source: DumpSource.RemoteSource): InputStream {
        val remote = "https://${source.uri.host.replace("-all", "")}/web/database/backup"
        val request = Request(Method.POST, remote)
            .form("master_pwd", dumpPassword.value)
            .form("name", source.name)
            .form("backup_format", "zip")
            .header("Content-Type", "application/x-www-form-urlencoded")
        return httpHandler(request).body.stream
    }

    private fun unzip(inputStream: InputStream) {
        val fileStoreTarget = Path(System.getProperty("user.home") + "/.local/share/Odoo/filestore/$name")

        ZipInputStream(inputStream).use { stream ->
            generateSequence { stream.nextEntry }.forEach { entry ->
                when {
                    entry.name == "dump.sql" -> {
                        restore(stream, entry.size)
                    }

                    entry.name == "manifest.json" -> {}
                    entry.name.startsWith("filestore/") -> {
                        val resolvedPath = fileStoreTarget.resolve(entry.name.removePrefix("filestore/")).normalize()
                        if (!resolvedPath.startsWith(fileStoreTarget)) {
                            error("Illegal path")
                        }
                        if (entry.isDirectory) {
                            resolvedPath.createDirectories()
                        } else {
                            resolvedPath.parent.createDirectories()
                            Files.copy(stream, resolvedPath)
                        }
                    }
                }
            }
        }
    }

    private fun restore(inputStream: ZipInputStream, totalSizeInBytes: Long) = runBlocking {
        process("createdb", name)

        val progress = terminal.progressAnimation {
            text("Restoring $name")
            progressBar()
            completed("B", includeTotal = true)
        }

        progress.updateTotal(totalSizeInBytes)
        progress.start()

        process(
            "psql",
            "-q",
            name,
            stdout = Redirect.SILENT,
            stderr = Redirect.SILENT,
            stdin = InputSource.FromStream { out ->
                var bytesCopied: Long = 0
                val buffer = ByteArray(size = 64 * 1024)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0) {
                    out.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    progress.update(bytesCopied)
                    bytes = inputStream.read(buffer)
                }
            },
        )

        process(
            "psql",
            "-d",
            name,
            "-c",
            listOf(
                "UPDATE ir_cron SET active = 'f'",
                "UPDATE ir_mail_server SET active = 'f'",
                "UPDATE res_users SET login = 'admin' WHERE id = 2",
                "UPDATE res_users SET password = 'admin'",
                "UPDATE res_users SET totp_secret = NULL",
                "UPDATE ir_config_parameter SET value = '2998-05-07 13:16:50' WHERE key = 'database.expiration_date'",
                "DELETE FROM ir_config_parameter WHERE key = 'database.expiration_reason'",
                "UPDATE ir_config_parameter SET value = '${UUID.randomUUID()}' WHERE key = 'database.uuid'",
            ).joinToString(";"),
            stdout = Redirect.SILENT,
            stderr = Redirect.SILENT,
        )

        progress.stop()
    }
}
