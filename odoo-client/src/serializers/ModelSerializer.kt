package com.github.hubvd.odootools.odoo.client.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.*

internal class ModelSerializer<T : Any>(originalSerializer: KSerializer<T>) : JsonTransformingSerializer<T>(
    originalSerializer,
) {
    override fun transformDeserialize(element: JsonElement) = buildJsonObject {
        val obj = element.jsonObject
        repeat(descriptor.elementsCount) { i ->
            val keyName = JsonNamingStrategy.SnakeCase.serialNameForJson(
                descriptor,
                i,
                descriptor.getElementName(i),
            )
            val elDescriptor = descriptor.getElementDescriptor(i)
            val nullable = elDescriptor.isNullable
            val jsonValue = obj[keyName]

            val isNull = jsonValue == null ||
                jsonValue == JsonNull ||
                elDescriptor.kind != PrimitiveKind.BOOLEAN &&
                jsonValue is JsonPrimitive &&
                !jsonValue.isString &&
                jsonValue.content == "false"

            if (isNull) {
                if (!nullable) {
                    throw IllegalArgumentException("$keyName is not nullable")
                } else {
                    put(keyName, null)
                    return@repeat
                }
            }

            when (elDescriptor.kind) {
                PrimitiveKind.INT -> {
                    val id = when (jsonValue) {
                        is JsonPrimitive -> {
                            jsonValue.jsonPrimitive.int
                        }

                        is JsonArray -> {
                            if (jsonValue.size != 2) {
                                throw IllegalArgumentException()
                            }
                            jsonValue.first().jsonPrimitive.int
                        }

                        else -> throw IllegalArgumentException()
                    }
                    put(keyName, id)
                }

                PrimitiveKind.LONG -> {
                    val id = when (jsonValue) {
                        is JsonPrimitive -> {
                            jsonValue.jsonPrimitive.long
                        }

                        is JsonArray -> {
                            if (jsonValue.size != 2) {
                                throw IllegalArgumentException()
                            }
                            jsonValue.first().jsonPrimitive.long
                        }

                        else -> throw IllegalArgumentException()
                    }
                    put(keyName, id)
                }

                else -> put(keyName, jsonValue!!)
            }
        }
    }
}
