@file:Suppress("ktlint:standard:filename")

package com.github.hubvd.odootools.libgit

import com.github.hubvd.odootools.libgit.Direction.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val f = File("/home/hubert/src/libgit2/script/api-docs/out.json")
    val j = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
        decodeEnumsCaseInsensitive = true
    }
    val groups = j.decodeFromString(JsonObject.serializer(), f.readText())["groups"]!!.jsonObject
    val structs = groups.values.map { j.decodeFromJsonElement(Group.serializer(), it.jsonObject) }
        .flatMap { it.apis.values }
        .filterIsInstance<Struct>()
    val repo = groups["repository"]!!.jsonObject
    val r = j.decodeFromJsonElement(Group.serializer(), repo)
    val s = r.apis.values.filterIsInstance<Struct>()
    structs.forEach {
        println(writeStruct(it))
    }
}

@Serializable
data class Group(
    val apis: Map<String, NativeKind>,
)

@Serializable
sealed interface NativeKind

@Serializable
@SerialName("function")
data class Function(val name: String, val returns: Type, val params: List<Type> = emptyList()) : NativeKind {
    override fun toString(): String = buildString {
        append(name)
        append('(')
        params.forEachIndexed { index, type ->
            when (type.direction) {
                In -> append('↓')
                Out -> append('↑')
                null -> {}
            }
            append(type.name)
            append(':')
            append(type.type)
            if (index != params.lastIndex) {
                append(", ")
            }
        }
        append(')')
        append(':')
        append(returns.type)
    }
}

@Serializable
@SerialName("enum")
data class Enum(val name: String, val members: List<JsonObject>) : NativeKind

@Serializable
@SerialName("struct")
data class Struct(val name: String, val opaque: Boolean = false, val members: List<Type> = emptyList()) : NativeKind

@Serializable
@SerialName("macro")
data class Macro(val name: String) : NativeKind

@Serializable
@SerialName("callback")
data class Callback(val name: String) : NativeKind

@Serializable
@SerialName("alias")
data class Alias(val name: String) : NativeKind

@Serializable
data class Type(val type: String, val name: String? = null, val direction: Direction? = null)

enum class Direction { In, Out }
