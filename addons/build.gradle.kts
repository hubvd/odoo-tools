plugins {
    id("spotless")
}

val blackVersion = project.findProperty("black.version") as String

spotless {
    python {
        target("**/*.py")
        black(blackVersion)
    }
}
