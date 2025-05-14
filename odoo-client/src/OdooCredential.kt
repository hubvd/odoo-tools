package com.github.hubvd.odootools.odoo.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = OdooCredentialSerializer::class)
sealed class OdooCredential {
    abstract val host: String
    abstract val userAgent: String?

    class JsonRpcCredential(
        override val host: String,
        override val userAgent: String?,
        val database: String,
        val userId: Long,
        val apiKey: String,
    ) : OdooCredential()

    class SessionCredential(override val host: String, override val userAgent: String?, val session: String) :
        OdooCredential()
}

object OdooCredentialSerializer : KSerializer<OdooCredential> {
    override fun serialize(encoder: Encoder, value: OdooCredential) = throw UnsupportedOperationException()

    override fun deserialize(decoder: Decoder): OdooCredential {
        val compositeDecoder = decoder.beginStructure(descriptor)

        var host: String? = null
        var database: String? = null
        var userId: Long? = null
        var apiKey: String? = null
        var session: String? = null
        var userAgent: String? = null

        while (true) {
            when (val index = compositeDecoder.decodeElementIndex(descriptor)) {
                0 -> host = compositeDecoder.decodeStringElement(descriptor, index)
                1 -> session = compositeDecoder.decodeStringElement(descriptor, index)
                2 -> database = compositeDecoder.decodeStringElement(descriptor, index)
                3 -> userId = compositeDecoder.decodeLongElement(descriptor, index)
                4 -> apiKey = compositeDecoder.decodeStringElement(descriptor, index)
                5 -> userAgent = compositeDecoder.decodeStringElement(descriptor, index)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }
        compositeDecoder.endStructure(descriptor)
        requireNotNull(host)

        if (session != null) {
            return OdooCredential.SessionCredential(host, userAgent, session)
        } else {
            requireNotNull(database)
            requireNotNull(userId)
            requireNotNull(apiKey)
            return OdooCredential.JsonRpcCredential(host, userAgent, database, userId, apiKey)
        }
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OdooCredential") {
        element<String>("host", isOptional = true)
        element<String>("session", isOptional = true)
        element<String>("database", isOptional = true)
        element<Long>("userId", isOptional = true)
        element<String>("apiKey", isOptional = true)
        element<String>("userAgent", isOptional = true)
    }
}
