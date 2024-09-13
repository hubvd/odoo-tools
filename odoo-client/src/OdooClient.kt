package com.github.hubvd.odootools.odoo.client

import com.github.hubvd.odootools.odoo.client.core.Condition
import com.github.hubvd.odootools.odoo.client.core.DomainBuilder
import com.github.hubvd.odootools.odoo.client.core.domain
import com.github.hubvd.odootools.odoo.client.serializers.ModelReferenceSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.http4k.core.*
import org.http4k.core.cookie.cookie
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters
import kotlin.random.Random

class OdooClient(
    private val credential: OdooCredential,
    client: HttpHandler,
    private val random: Random = Random.Default,
) {

    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    private val client = ClientFilters.SetHostFrom(Uri.of(credential.host))
        .then(RequestFilters.SetHeader("Content-Type", "application/json"))
        .then(client)

    fun <T> rpc(
        model: String,
        method: String,
        vararg arguments: JsonElement,
        kwargs: JsonObject = buildJsonObject { },
        deserializer: DeserializationStrategy<T>,
    ): T {
        val body = body(model, method, arguments = arguments, kwargs)
        val request = request().body(json.encodeToString(JsonObject.serializer(), body))
        val response = client(request)
        if (!response.status.successful) {
            throw RuntimeException(response.status.toString()) // TODO
        }

        val jsonResponse = json.decodeFromString(JsonObject.serializer(), response.bodyString())
        jsonResponse["error"]?.run {
            val data = this.jsonObject["data"]!!.jsonObject
            val errorName = data["name"]!!.jsonPrimitive.content
            data["debug"]?.jsonPrimitive?.contentOrNull?.let { System.err.println(it) }
            throw RuntimeException("Bad response $errorName")
        }
        return json.decodeFromJsonElement(deserializer, jsonResponse["result"]!!)
    }

    fun <T : Any> read(model: String, ids: IntArray, deserializer: KSerializer<T>): List<T> = rpc(
        model,
        "read",
        json.encodeToJsonElement(IntArraySerializer(), ids),
        json.encodeToJsonElement(ListSerializer(String.serializer()), deserializer.fields()),
        kwargs = buildJsonObject {
            if (!deserializer.hasModelReference()) {
                put("load", "")
            }
        },
        deserializer = ListSerializer(deserializer),
    )

    fun <T : Any> searchRead(
        model: String,
        deserializer: KSerializer<T>,
        context: JsonObject? = null,
        domain: JsonElement,
        offset: Int = 0,
        limit: Int = -1,
        order: String?,
    ): List<T> = rpc(
        model,
        "search_read",
        domain,
        Json.encodeToJsonElement(
            ListSerializer(String.serializer()),
            deserializer.fields(),
        ),
        kwargs = buildJsonObject {
            if (offset != 0) {
                put("offset", offset)
            }
            if (limit != -1) {
                put("limit", limit)
            }
            if (order != null) {
                put("order", order)
            }
            if (!deserializer.hasModelReference()) {
                put("load", "")
            }
            context?.let {
                put("context", it)
            }
        },
        deserializer = ListSerializer(deserializer),
    )

    private fun body(
        model: String,
        method: String,
        vararg arguments: JsonElement,
        kwargs: JsonObject = buildJsonObject { },
    ): JsonObject = when (credential) {
        is OdooCredential.SessionCredential -> {
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", random.nextInt())
                put("method", "call")
                putJsonObject("params") {
                    put("model", model)
                    put("method", method)
                    putJsonArray("args") {
                        arguments.forEach { add(it) }
                    }
                    put("kwargs", kwargs)
                }
            }
        }

        is OdooCredential.JsonRpcCredential -> {
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", random.nextInt())
                put(
                    "params",
                    buildJsonObject {
                        put("service", "object")
                        put("method", "execute_kw")
                        put(
                            "args",
                            buildJsonArray {
                                add(credential.database)
                                add(credential.userId)
                                add(credential.apiKey)
                                add(model)
                                add(method)
                                addJsonArray {
                                    arguments.forEach { add(it) }
                                }
                                add(kwargs)
                            },
                        )
                    },
                )
            }
        }
    }

    private fun request() = if (credential is OdooCredential.SessionCredential) {
        Request(Method.POST, "/web/dataset/call_kw")
            .cookie("session_id", credential.session)
            .cookie("cids", "1")
            .cookie("frontend_lang", "en_US")
    } else {
        Request(Method.POST, "/jsonrpc")
    }

    private fun KSerializer<*>.fields(): List<String> = buildList(descriptor.elementsCount) {
        repeat(descriptor.elementsCount) {
            add(JsonNamingStrategy.SnakeCase.serialNameForJson(descriptor, it, descriptor.getElementName(it)))
        }
    }

    private fun KSerializer<*>.hasModelReference(): Boolean {
        repeat(descriptor.elementsCount) {
            if (descriptor.getElementDescriptor(it) == ModelReferenceSerializer.descriptor) {
                return true
            }
        }
        return false
    }
}

inline fun <reified T : Any> OdooClient.read(model: String, ids: IntArray): List<T> = read(
    model = model,
    ids = ids,
    deserializer = serializer<T>(),
)

inline fun <reified T : Any> OdooClient.searchRead(
    model: String,
    context: JsonObject? = null,
    offset: Int = 0,
    limit: Int = -1,
    order: String? = null,
    noinline domain: DomainBuilder.() -> Condition,
): List<T> = searchRead(
    model = model,
    deserializer = serializer<T>(),
    context = context,
    domain = domain(domain),
    limit = limit,
    offset = offset,
    order = order,
)
