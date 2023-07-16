plugins {
    id("spotless")
}

spotless {
    python {
        target("**/*.py")
        black("23.7.0")
    }
}
