package com.github.hubvd.odootools.odoo.client.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ModelReferenceSerializerTest {

    @Test
    fun `deserialize reference`() {
        assertThat(Json.decodeFromString(ModelReference.serializer(), "[1, \"test\"]"))
            .isEqualTo(ModelReference(1, "test"))
    }
}
