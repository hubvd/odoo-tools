package com.github.hubvd.odootools.actions.utils

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class FakeGitTest {

    private val repo = FakeGit(
        mapOf(
            Path("/odoo-tools") to
                """
        c738a69c61d4033976b275d9923769e8f14f81ae Update libs
        c6ebad232f7d4ab0612a55b48119ad630acba585 Fetch branches in parallel
        15727a24f53baa839519c3027b8b08c4c9bead86 Add new command + BrowserService
        b70318b223a2bb18dec30f505777f92c0168b60f Automatically generate subcommand launchers
        4428548f048cb6f54cc66708dcb928eea3c6558a Add pydevd StrPresentationProvider for lxml
        6c88b93f14d3041b9d081062de0cd2e73e2a2a10 Huge commit with some stuff
        7729da20df373ea102670d0ecda586156b42bcda Restore cursor after exit
        a7d7994cff940a306bf71a26a2a668ac72451313 Add debug option
                """.trimIndent(),
        ),
    ).open(Path("/odoo-tools"))

    @Test
    fun `commit title`() {
        assertThat(repo.commitTitle("a7d7994cff940a306bf71a26a2a668ac72451313"))
            .isEqualTo("Add debug option")
    }

    @Test
    fun `commits between`() {
        assertThat(
            repo.commitsBetween("4428548f048cb6f54cc66708dcb928eea3c6558a", "c6ebad232f7d4ab0612a55b48119ad630acba585"),
        )
            .containsExactly(
                Commit(
                    hash = "c6ebad232f7d4ab0612a55b48119ad630acba585",
                    title = "Fetch branches in parallel",
                ),
                Commit(
                    hash = "15727a24f53baa839519c3027b8b08c4c9bead86",
                    title = "Add new command + BrowserService",
                ),
                Commit(
                    hash = "b70318b223a2bb18dec30f505777f92c0168b60f",
                    title = "Automatically generate subcommand launchers",
                ),
            )
    }
}
