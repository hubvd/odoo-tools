package com.github.hubvd.odootools.config

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

typealias ShellPath =
    @Serializable(PathSerializer::class)
    Path

object PathSerializer : KSerializer<Path> {
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

interface Config {
    fun <T> get(section: String, deserializer: DeserializationStrategy<T>): T
}

private object TomlConfig : Config {
    private val CONTENT = Path(System.getProperty("user.home"), ".config/odoo/config.toml").readText()

    override fun <T> get(section: String, deserializer: DeserializationStrategy<T>): T = Toml.partiallyDecodeFromString(
        deserializer,
        CONTENT,
        section,
        TomlInputConfig(ignoreUnknownNames = true),
    )
}

val CONFIG_MODULE by DI.Module {
    bind<Config>() with singleton { TomlConfig }
}
