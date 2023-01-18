plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        ktlint("0.48.0")
    }
}
