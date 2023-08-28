package com.github.hubvd.odootools.odoo.client.serializers

import com.github.hubvd.odootools.odoo.client.core.ModelReference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal object ModelReferenceSerializer : KSerializer<ModelReference> {
    override fun deserialize(decoder: Decoder): ModelReference {
        val element = (decoder as JsonDecoder).decodeJsonElement().jsonArray
        return ModelReference(element[0].jsonPrimitive.long, element[1].jsonPrimitive.content)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ModelReference") {
        element<Long>("id")
        element<String>("name")
    }

    override fun serialize(encoder: Encoder, value: ModelReference) {
        TODO("Not yet implemented")
    }
}
