plugins {
    id("cli-application")
    id("launcher-checksums")
}

cli {
    name = "odoo"
    mainClass = "com.github.hubvd.odootools.odoo.OdooKt"
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))
    implementation(libs.coroutines.core)
    implementation(libs.process)
    implementation(libs.xmlbuilder)
    implementation(libs.kodein.di)
}

spotless {
    python {
        target("resources/launcher/**/*.py")
        black("23.3.0")
    }
}
