package com.github.hubvd.odootools.odoo.client.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@Serializable
private data class TestModel(val displayName: String?, val m2o: Long, val test: String?)

@Serializable
private data class TestModelWithReference(val id: Long, val m2o: ModelReference)

class ModelSerializerTest {

    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    private fun <T : Any> deserializeModel(@Language("json") body: String, serializer: KSerializer<T>): T {
        return json.decodeFromString(ModelSerializer(serializer), body)
    }

    @Test
    fun `deserializer simple model`() {
        assertThat(deserializeModel("""{"display_name":  "test", "m2o":  1}""", TestModel.serializer()))
            .isEqualTo(TestModel(displayName = "test", m2o = 1L, test = null))
    }

    @Test
    fun `deserialize model with reference to id`() {
        assertThat(deserializeModel("""{"display_name":  "test", "m2o":  [1, "m2one name"]}""", TestModel.serializer()))
            .isEqualTo(TestModel(displayName = "test", m2o = 1L, test = null))
    }

    @Test
    fun `null as false`() {
        assertThat(deserializeModel("""{"display_name":  "test", "m2o":  1, "test": false}""", TestModel.serializer()))
            .isEqualTo(TestModel(displayName = "test", m2o = 1L, test = null))
    }

    @Test
    fun `deserialize model with reference`() {
        assertThat(deserializeModel("""{"id":  "1", "m2o":  [1, "m2one name"]}""", TestModelWithReference.serializer()))
            .isEqualTo(TestModelWithReference(id = 1, m2o = ModelReference(1, "m2one name")))
    }
}
