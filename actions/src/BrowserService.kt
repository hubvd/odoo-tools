package com.github.hubvd.odootools.actions

interface BrowserService {
    fun open(url: String)
    fun firefox(url: String)
    fun chrome(url: String)
}

class BrowserServiceImpl(private val config: BrowserConfig) : BrowserService {
    override fun open(url: String) {
        when (config.default) {
            "firefox" -> firefox(url)
            "chrome", "chromium" -> chrome(url)
            else -> error("Invalid browser")
        }
    }

    override fun firefox(url: String) {
        ProcessBuilder(*config.firefox.toTypedArray(), url)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    override fun chrome(url: String) {
        ProcessBuilder(*config.chrome.toTypedArray(), url)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }
}
