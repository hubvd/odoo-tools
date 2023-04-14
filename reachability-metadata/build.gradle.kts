plugins {
    id("spotless")
}

repositories { mavenCentral() }

spotless {
    json {
        target("**/*.json")
        gson()
            .indentWithSpaces(4)
            .sortByKeys()
    }
}
