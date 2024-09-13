package com.github.hubvd.odootools.odoo.client

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.github.hubvd.odootools.odoo.client.assertions.isJsonEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OdooClientTest {

    @Serializable
    private data class ResUser(val id: Long, val displayName: String)

    @Test
    fun `simple read`() {
        var request: Request? = null

        val odoo = OdooClient(
            OdooCredential.JsonRpcCredential(
                host = "https://example.com",
                database = "test",
                userId = 1L,
                apiKey = "dummy",
            ),
            client = {
                request = it
                Response(Status.OK).body("""{"result": [{"id": 1, "display_name": "name"}]}""")
            },
            random = Random(1),
        )

        assertThat(odoo.read<ResUser>("res.user", intArrayOf(1)))
            .containsExactly(ResUser(id = 1, displayName = "name"))

        assertThat(request).isNotNull().all {
            prop("method") { it.method }.isEqualTo(Method.POST)
            prop("url") { it.uri.toString() }.isEqualTo("https://example.com/jsonrpc")
            prop("headers") { it.headers }.isEqualTo(
                listOf("Host" to "example.com", "Content-Type" to "application/json"),
            )
            prop("body") {
                Json.decodeFromString(JsonObject.serializer(), it.bodyString())
            }.isJsonEqualTo(
                """
                    {
                        "jsonrpc":"2.0",
                        "id":600123930,
                        "params":{"service":"object","method":"execute_kw","args":["test",1,"dummy","res.user","read",[[1],["id","display_name"]],{"load":""}]}
                    }
                """.trimIndent(),
            )
        }
    }
}
