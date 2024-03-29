package com.github.hubvd.odootools.actions.kitty

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OsWindow(
    val id: Int,
    val platformWindowId: Int?,
    val isFocused: Boolean,
    val isActive: Boolean,
    val lastFocused: Boolean,
    val tabs: List<Tab>,
    val wmClass: String,
    val wmName: String,
    val backgroundOpacity: Float,
)

@Serializable
data class Tab(
    val id: Int,
    val isFocused: Boolean,
    val isActive: Boolean,
    val title: String,
    val layout: String,
    val layoutState: Map<String, JsonElement>,
    val layoutOpts: Map<String, JsonElement>,
    val enabledLayouts: List<String>,
    val windows: List<Window>,
    val groups: List<Map<String, JsonElement>>,
    val activeWindowHistory: List<Int>,
)

@Serializable
data class Window(
    val id: Int,
    val isFocused: Boolean,
    val isActive: Boolean,
    val title: String,
    val pid: Long?,
    val cwd: String,
    val cmdline: List<String>,
    val env: Map<String, String>,
    val foregroundProcesses: List<Process>,
    val isSelf: Boolean,
    val lines: Int,
    val columns: Int,
    val userVars: Map<String, String>,
    val atPrompt: Boolean,
    val createdAt: Long,
)

@Serializable
data class Process(
    val cwd: String?,
    val pid: Long,
    val cmdline: List<String>?,
)
