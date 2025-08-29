package com.github.hubvd.odootools.actions.commands

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import com.github.hubvd.odootools.actions.utils.BrowserService

class OpenerCommandTest {

    class BrowserServiceRecorder : BrowserService {
        var lastOpenedUrl: String? = null
            private set

        override fun open(url: String) {
            lastOpenedUrl = url
        }

        override fun firefox(url: String) = Unit
        override fun chrome(url: String) = Unit
    }

    @Test
    fun openTicketFromImplicitId() {
        val recorder = BrowserServiceRecorder()
        val cmd = OpenerCommand(recorder)
        cmd.test("123")
        assertThat(recorder.lastOpenedUrl).isEqualTo("https://www.odoo.com/odoo/49/tasks/123")
    }

    @Test
    fun openTicketFromId() {
        val recorder = BrowserServiceRecorder()
        val cmd = OpenerCommand(recorder)
        cmd.test("-t 123")
        assertThat(recorder.lastOpenedUrl).isEqualTo("https://www.odoo.com/odoo/49/tasks/123")
    }

    @Test
    fun openErrorFromId() {
        val recorder = BrowserServiceRecorder()
        val cmd = OpenerCommand(recorder)
        cmd.test("-r 123")
        assertThat(recorder.lastOpenedUrl).isEqualTo("https://runbot.odoo.com/odoo/runbot.build.error/123")
    }

    @Test
    fun openImplicitPr() {
        val recorder = BrowserServiceRecorder()
        val cmd = OpenerCommand(recorder)
        cmd.test("'odoo/odoo#123456'")
        assertThat(recorder.lastOpenedUrl).isEqualTo("https://github.com/odoo/odoo/pull/123456")
    }

    @Test
    fun openAllErrors() {
        val recorder = BrowserServiceRecorder()
        val cmd = OpenerCommand(recorder)
        cmd.test("-ra")
        assertThat(recorder.lastOpenedUrl).isEqualTo("https://runbot.odoo.com/odoo/error")
    }
}
