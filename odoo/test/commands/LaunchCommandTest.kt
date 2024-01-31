package com.github.hubvd.odootools.odoo.commands

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import com.github.hubvd.odootools.odoo.ODOO_MODULE
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.odoo.actions.Action
import com.github.hubvd.odootools.odoo.actions.ActionProvider
import com.github.hubvd.odootools.workspace.FakeWorkspaces
import com.github.hubvd.odootools.workspace.StaticWorkspace
import com.github.hubvd.odootools.workspace.Workspaces
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import java.nio.file.Path
import kotlin.io.path.div

class LaunchCommandTest {

    companion object {

        @TempDir
        private lateinit var tempDir: Path
        private lateinit var workspaceRoot: Path

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            workspaceRoot = tempDir / "saas-16.3"
        }
    }

    private val workspaces = FakeWorkspaces(
        listOf(
            StaticWorkspace(
                name = "saas-16.3",
                path = workspaceRoot,
                version = 16.3f,
                base = "saas-16.3",
            ),
        ),
        current = "saas-16.3",
        default = "saas-16.3",
    )

    private class ActionRecorder : Action {
        lateinit var configuration: RunConfiguration

        override fun run(configuration: RunConfiguration) {
            this.configuration = configuration
        }
    }

    private fun testCommand(vararg args: String): RunConfiguration {
        val actionRecorder = ActionRecorder()
        val di = DI {
            import(ODOO_MODULE, allowOverride = true)
            bind<Workspaces>(overrides = true) { instance(workspaces) }
            bind<ActionProvider>(overrides = true) { instance(ActionProvider { _, _ -> actionRecorder }) }
        }
        val launchCommand by di.instance<LaunchCommand>()
        launchCommand.parse(args.toList())
        return actionRecorder.configuration
    }

    @Test
    fun `no args`() {
        val result = testCommand()
        assertThat(result.args).containsExactlyInAnyOrder(
            "--addons-path=odoo/addons,enterprise,~/odoo-tools/addons",
            "--database=saas-16.3",
            "--http-port=1630",
            "--limit-time-cpu=99999",
            "--limit-time-real=99999",
        )
        assertThat(result.env).containsOnly("ODOO_WORKSPACE" to workspaceRoot.toString())
        assertThat(result.cwd).isEqualTo(workspaceRoot)
    }

    @Test
    fun `test tags`() {
        val result = testCommand("--test-tags", "/base")
        assertThat(result.args).containsExactlyInAnyOrder(
            "--addons-path=odoo/addons,enterprise,~/odoo-tools/addons",
            "--database=saas-16.3-test",
            "--http-port=1635",
            "--limit-time-cpu=99999",
            "--limit-time-real=99999",
            "--max-cron-thread=0",
            "--test-enable",
            "--test-tags=/base",
            "--stop-after-init",
            "--init=base",
            "--log-level=test",
            "--log-handler=werkzeug:ERROR",
        )
        assertThat(result.env).containsOnly("ODOO_WORKSPACE" to workspaceRoot.toString())
        assertThat(result.cwd).isEqualTo(workspaceRoot)
    }

    @Test
    fun `custom flags`() {
        val result = testCommand("--test-tags", "/base", "--watch", "--step-delay", "150")
        assertThat(result.args).containsExactlyInAnyOrder(
            "--addons-path=odoo/addons,enterprise,~/odoo-tools/addons",
            "--database=saas-16.3-test",
            "--http-port=1635",
            "--limit-time-cpu=99999",
            "--limit-time-real=99999",
            "--max-cron-thread=0",
            "--test-enable",
            "--test-tags=/base",
            "--stop-after-init",
            "--init=base",
            "--log-level=test",
            "--log-handler=werkzeug:ERROR",
        )
        assertThat(result.env).containsOnly(
            "ODOO_WORKSPACE" to workspaceRoot.toString(),
            "QUNIT_WATCH" to "1",
            "STEP_DELAY" to "150",
        )
        assertThat(result.cwd).isEqualTo(workspaceRoot)
    }
}
