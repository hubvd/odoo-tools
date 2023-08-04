package com.github.hubvd.odootools.actions.utils

import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.readText

class LinuxProcessHandle(private val delegate: ProcessHandle) : ProcessHandle by delegate {

    fun loginuid(): Long = try {
        Path("/proc/${delegate.pid()}/loginuid").readText().toLong()
    } catch (e: AccessDeniedException) {
        -1
    } catch (e: NoSuchFileException) {
        -1
    }

    fun cwd(): Path = Path("/proc/${delegate.pid()}/cwd").toRealPath()

    override fun info() = object : ProcessHandle.Info {

        override fun command() = try {
            Optional.of(Path("/proc/${delegate.pid()}/exe").toRealPath().toString())
        } catch (e: AccessDeniedException) {
            Optional.empty()
        } catch (e: NoSuchFileException) {
            Optional.empty()
        }

        override fun commandLine(): Optional<String> {
            TODO("Not yet implemented")
        }

        override fun arguments() = try {
            Optional.of(Path("/proc/${delegate.pid()}/cmdline").readText().split('\u0000').toTypedArray<String>())
        } catch (e: AccessDeniedException) {
            Optional.empty()
        } catch (e: NoSuchFileException) {
            Optional.empty()
        }

        override fun startInstant(): Optional<Instant> {
            TODO("Not yet implemented")
        }

        override fun totalCpuDuration(): Optional<Duration> {
            TODO("Not yet implemented")
        }

        override fun user(): Optional<String> {
            TODO("Not yet implemented")
        }
    }
}
