package com.github.hubvd.odootools.odoo.client.core

import kotlinx.serialization.json.*

private fun JsonArrayBuilder.genericAdd(any: Any?) {
    when (any) {
        is Boolean -> add(any)
        is String -> add(any)
        is Number -> add(any)
        else -> TODO()
    }
}

private fun jsonCondition(field: String, operator: String, value: Any) = Condition(
    buildJsonArray {
        addJsonArray {
            add(field)
            add(operator)
            when (value) {
                is Boolean -> add(value)
                is String -> add(value)
                is Number -> add(value)
                is IntArray -> addJsonArray {
                    value.forEach { add(it) }
                }

                is Array<*> -> addJsonArray {
                    value.forEach {
                        genericAdd(it)
                    }
                }

                is Collection<*> -> addJsonArray {
                    value.forEach {
                        genericAdd(it)
                    }
                }

                else -> TODO()
            }
        }
    },
)

fun domain(block: DomainBuilder.() -> Condition): JsonElement {
    val builder = object : DomainBuilder {
        override fun String.eq(value: Any) = jsonCondition(this, "=", value)
        override fun String.`in`(value: Any) = jsonCondition(this, "in", value)
        override fun String.childOf(value: Any) = jsonCondition(this, "child_of", value)
        override fun String.le(value: Any) = jsonCondition(this, "<=", value)
        override fun String.ge(value: Any) = jsonCondition(this, ">=", value)
        override fun String.like(value: Any) = jsonCondition(this, "like", value)
    }

    val condition = block(builder)
    return condition.jsonElement
}

private val TRUE_LEAF = Condition(
    buildJsonArray {
        add(1)
        add("=")
        add(1)
    },
)

private val FALSE_LEAF = Condition(
    buildJsonArray {
        add(0)
        add("=")
        add(1)
    },
)

interface DomainBuilder {
    infix fun String.eq(value: Any): Condition
    infix fun String.`in`(value: Any): Condition
    infix fun String.childOf(value: Any): Condition
    infix fun String.le(value: Any): Condition
    infix fun String.ge(value: Any): Condition
    infix fun String.like(value: Any): Condition
}

data class Condition(val jsonElement: JsonElement) {

    private fun combine(operator: String, unit: Condition, zero: Condition, domains: List<Condition>): Condition {
        val result = mutableListOf<JsonElement>()
        var count = 0
        if (domains == listOf(unit)) return unit
        for (domain in domains) {
            if (domain == unit) continue
            if (domain == zero) return zero

            when (val el = domain.jsonElement) {
                is JsonArray -> el.forEach {
                    result.add(it)
                }

                else -> error("Unexpected type for json element $el")
            }
            count++
        }
        repeat(count - 1) {
            result.add(0, JsonPrimitive(operator))
        }
        if (result.isEmpty()) {
            return unit
        }
        return Condition(
            buildJsonArray {
                result.forEach { add(it) }
            },
        )
    }

    infix fun and(other: Condition): Condition = combine("&", TRUE_LEAF, FALSE_LEAF, listOf(this, other))

    infix fun or(other: Condition): Condition = combine("|", FALSE_LEAF, TRUE_LEAF, listOf(this, other))
}
