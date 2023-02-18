plugins {
    id("spotless")
}

spotless {
    python {
        target("**/*.py")
        black("22.12.0")
    }
}
