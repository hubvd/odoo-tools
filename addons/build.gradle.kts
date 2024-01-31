plugins {
    id("spotless")
}

spotless {
    python {
        target("**/*.py")
        black("23.12.1")
    }
}
