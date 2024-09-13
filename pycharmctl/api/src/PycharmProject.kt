package com.github.hubvd.odootools.pycharmctl.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
sealed class PycharmProject {
    @Serializable
    @SerialName("path")
    data class PycharmProjectByPath(val path: ShellPath) : PycharmProject()

    @Serializable
    @SerialName("name")
    data class PycharmProjectByName(val name: String) : PycharmProject()

    @Serializable
    @SerialName("full")
    data class PycharmProjectInfo(val name: String, val path: ShellPath) : PycharmProject()
}

// FIXME: export module for this I guess ?
typealias ShellPath =
    @Serializable(PathSerializer::class)
    Path

private object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Path {
        val value = decoder.decodeString()
        return Path(
            when {
                value.startsWith("~/") -> value.replaceRange(0..1, System.getProperty("user.home") + "/")
                value == "~" -> System.getProperty("user.home")
                else -> value
            },
        )
    }
}
