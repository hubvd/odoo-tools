plugins {
    id("spotless")
}

spotless {
    python {
        target("**/*.py")
        black("24.3.0")
    }
}
