package com.github.hubvd.odootools.actions

import com.github.hubvd.odootools.config.Config
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

@JvmInline
value class UserId(val value: Long)

@JvmInline
value class Secret(val value: String) {
    override fun toString() = "***"
}

@Serializable
data class BrowserConfig(
    val firefox: List<String> = listOf("firefox"),
    val chrome: List<String> = listOf("chromium"),
    val default: String = "firefox",
)

@Serializable
data class ActionsConfig(
    val dumpPassword: String,
    val apiKey: String,
    val userId: Long,
    val githubApiKey: String,
    val browsers: BrowserConfig = BrowserConfig(),
)

val ACTIONS_CONFIG_MODULE = DI.Module("actions_config") {
    bind { singleton { Config.get("actions", ActionsConfig.serializer()) } }

    // FIXME: deserialize directly into value classes
    bind { singleton { UserId(instance<ActionsConfig>().userId) } }
    bind(tag = "odoo_dump_password") { singleton { Secret(instance<ActionsConfig>().dumpPassword) } }
    bind(tag = "github_api_key") { singleton { Secret(instance<ActionsConfig>().githubApiKey) } }
}
