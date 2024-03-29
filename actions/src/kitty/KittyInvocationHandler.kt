package com.github.hubvd.odootools.actions.kitty

import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class KittyInvocationHandler(private val socketAddress: String) : InvocationHandler {

    companion object {
        private const val PREFIX = "\u001BP@kitty-cmd"
        private const val SUFFIX = "\u001B\\"
        private val camelCaseRe = Regex("([a-z])([A-Z])")
        private val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }
    }

    private fun camelToSnakeCase(name: String) = camelCaseRe.replace(name, "\$1_\$2").lowercase()

    private fun cast(arg: Any?): JsonElement {
        if (arg == null) return JsonNull
        return when (arg) {
            is String -> JsonPrimitive(arg)
            is Boolean -> JsonPrimitive(arg)
            is Array<*> -> JsonArray(arg.map { cast(it) })
            is Iterable<*> -> JsonArray(arg.map { cast(it) })
            else -> TODO(arg::class.java.name)
        }
    }

    private fun channel(): SocketChannel {
        val address = UnixDomainSocketAddress.of(socketAddress)
        return SocketChannel.open(StandardProtocolFamily.UNIX).apply { connect(address) }
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val noResponse = method.returnType == Void.TYPE
        val request = buildRequest(args, method, noResponse)

        val channel = channel()
        val buffer = ByteBuffer.wrap(request)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }

        if (noResponse) {
            channel.close()
            return null
        }

        val response = readResponse(channel)

        channel.close()

        val json = Json.decodeFromString(JsonObject.serializer(), response)
        val ok = json["ok"]?.takeIf { it is JsonPrimitive }?.jsonPrimitive?.booleanOrNull == true
        if (ok) {
            val rawData = json["data"]!!.jsonPrimitive.content
            val serializer = format.serializersModule.serializer(method.genericReturnType)
            return format.decodeFromString(serializer, rawData)
        } else {
            println(json["error"])
            println(json["tb"]?.jsonPrimitive?.content)
        }

        return json
    }

    private fun buildRequest(args: Array<out Any>?, method: Method, noResponse: Boolean): ByteArray {
        val payload = buildJsonObject {
            if (args == null) return@buildJsonObject
            method.parameters.zip(args).forEach { (param, arg) ->
                val value = cast(arg)
                if (value != JsonNull) {
                    put(camelToSnakeCase(param.name), value)
                }
            }
        }

        val message = buildJsonObject {
            put("cmd", camelToSnakeCase(method.name))
            putJsonArray("version") {
                add(0)
                add(14)
                add(2)
            }
            if (noResponse) {
                put("no_response", true)
            }
            put("payload", payload)
        }

        return "$PREFIX$message$SUFFIX".toByteArray()
    }

    private fun readResponse(channel: SocketChannel): String {
        val response = ByteArrayOutputStream().use { stream ->
            val buff = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (true) {
                read = channel.read(buff)
                buff.flip()
                stream.write(buff.array(), 0, read)
                if (read <= 0 || buff.limit() < buff.capacity()) break // FIXME
            }
            val payload = stream.toString(Charsets.UTF_8)
            if (payload.startsWith(PREFIX) && payload.endsWith(SUFFIX)) {
                payload.substring(PREFIX.length..<payload.length - SUFFIX.length)
            } else {
                TODO()
            }
        }
        return response
    }
}
