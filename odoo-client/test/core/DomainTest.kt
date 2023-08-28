package com.github.hubvd.odootools.odoo.client.core

import assertk.assertThat
import com.github.hubvd.odootools.odoo.client.assertions.isJsonEqualTo
import org.junit.jupiter.api.Test

class DomainTest {

    @Test
    fun `simple domain`() {
        assertThat(domain { "id" eq 1 }).isJsonEqualTo("""[["id","=",1]]""")
    }

    @Test
    fun `and condition`() {
        assertThat(
            domain {
                ("id" eq 1) and ("name" eq "test")
            },
        ).isJsonEqualTo("""["&",["id","=",1],["name","=","test"]]""")
    }

    @Test
    fun `complex domain`() {
        assertThat(
            domain {
                "name" eq "Admin" and (
                    "country_id" eq 1 or (
                        "country_id" eq 2 and
                            ("has_message" eq true)
                        )
                    )
            },
        ).isJsonEqualTo(
            """
            [
                "&",
                ["name","=","Admin"],
                "|",
                ["country_id","=",1],
                "&",
                ["country_id","=",2],
                ["has_message","=",true]
            ]
            """.trimIndent(),
        )
    }
}
