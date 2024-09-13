package com.github.hubvd.odootools.odoo

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class OptionCompute(
    val name: String,
    initialValue: String?,
    private val block: (() -> String?)? = null,
) : ReadOnlyProperty<CliGenerator, String?> {
    private var value = initialValue
    private var hasBeenCalled = false

    fun get(): String? {
        if (value == null && !hasBeenCalled && block != null) {
            value = block()
            hasBeenCalled = true
        }
        return value
    }

    override fun getValue(thisRef: CliGenerator, property: KProperty<*>) = get()
}

class EnvCompute(
    val name: String,
    private val block: (() -> String?)? = null,
) : ReadOnlyProperty<CliGenerator, String?> {
    private var value: String? = null
    private var hasBeenCalled = false

    fun get(): String? {
        if (!hasBeenCalled && block != null) {
            value = block()
            hasBeenCalled = true
        }
        return value
    }

    override fun getValue(thisRef: CliGenerator, property: KProperty<*>) = get()
}

class FlagCompute(
    val name: String,
    initialValue: Boolean,
    private val block: (() -> Boolean)? = null,
) : ReadOnlyProperty<CliGenerator, Boolean> {
    private var value = initialValue
    private var hasBeenCalled = false

    fun get(): Boolean {
        if (!value && !hasBeenCalled && block != null) {
            value = block()
            hasBeenCalled = true
        }
        return value
    }

    override fun getValue(thisRef: CliGenerator, property: KProperty<*>) = get()
}

abstract class CliGenerator(
    options: Map<String, String>,
    flags: Set<String>,
) {

    private val options: MutableMap<String, String> = options.toMap(HashMap())
    private val flags = flags.toHashSet()
    private val camelCaseRe = Regex("([a-z0-9])([A-Z])")

    val registeredOptions = ArrayList<OptionCompute>()
    val registeredFlags = ArrayList<FlagCompute>()
    val registeredEnv = ArrayList<EnvCompute>()
    val registeredEffects = ArrayList<() -> Unit>()

    fun effect(block: () -> Unit) {
        registeredEffects += block
    }

    fun option(
        block: (() -> String?)? = null,
    ): PropertyDelegateProvider<CliGenerator, ReadOnlyProperty<CliGenerator, String?>> =
        PropertyDelegateProvider { thisRef, property ->
            val name = camelCaseRe.replace(property.name, "\$1-\$2").lowercase()
            OptionCompute(name, options[name], block).also { registeredOptions += it }
        }

    fun flag(
        block: (() -> Boolean)? = null,
    ): PropertyDelegateProvider<CliGenerator, ReadOnlyProperty<CliGenerator, Boolean>> =
        PropertyDelegateProvider { thisRef, property ->
            val name = camelCaseRe.replace(property.name, "\$1-\$2").lowercase()
            FlagCompute(name, flags.contains(name), block).also { registeredFlags += it }
        }

    fun env(
        name: String? = null,
        block: () -> String?,
    ): PropertyDelegateProvider<CliGenerator, ReadOnlyProperty<CliGenerator, String?>> =
        PropertyDelegateProvider { thisRef, property ->
            val name = name ?: camelCaseRe.replace(property.name, "\$1_\$2").uppercase()
            EnvCompute(name, block).also { registeredEnv += it }
        }
}
