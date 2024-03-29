package com.github.hubvd.odootools.actions.utils

interface WindowManager {
    fun raise(resourceClass: String)

    companion object {
        operator fun invoke(): WindowManager {
            val desktop = System.getenv("XDG_CURRENT_DESKTOP")
            val session = System.getenv("XDG_SESSION_DESKTOP")
            return when {
                desktop == "Hyprland" -> Hyprctl()
                session == "sway" -> SwayMsg()
                else -> TODO("Unsupported desktop: XDG_CURRENT_DESKTOP=$desktop XDG_SESSION_DESKTOP=$session")
            }
        }
    }
}

class Hyprctl : WindowManager {
    override fun raise(resourceClass: String) {
        ProcessBuilder("hyprctl", "dispatch", "focuswindow", resourceClass)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor()
    }
}

class SwayMsg : WindowManager {
    override fun raise(resourceClass: String) {
        ProcessBuilder("swaymsg", """[app_id="$resourceClass"]""", "focus")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor()
    }
}
