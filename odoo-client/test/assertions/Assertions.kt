package com.github.hubvd.odootools.odoo.client.assertions

import assertk.Assert
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.intellij.lang.annotations.Language

internal fun Assert<JsonElement>.isJsonEqualTo(@Language("json") expected: String) {
    isEqualTo(Json.decodeFromString(JsonElement.serializer(), expected))
}
