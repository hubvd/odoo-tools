package com.github.hubvd.odootools.actions.kitty

interface KittyCommands {
    fun launch(
        vararg args: String = emptyArray(),
        match: String? = null,
        windowTitle: String? = null,
        cwd: String? = null,
        `var`: List<String> = emptyList(),
        type: String? = null,
        hold: Boolean = false,
        location: String? = null,
    )

    fun focusWindow(match: String?)

    fun closeWindow(match: String?)

    fun ls(): List<OsWindow>
}
