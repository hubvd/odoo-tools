plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        ktlint("0.48.0")
            .setUseExperimental(true)
            .editorConfigOverride(
                mapOf(
                    "disabled_rules" to "no-wildcard-imports,import-ordering",
                    /* TODO: make a PR to support the following
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_import-ordering" to "disabled",
                     */
                ),
            )
    }
}
