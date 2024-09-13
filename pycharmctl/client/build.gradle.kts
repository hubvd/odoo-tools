plugins {
    id("kotlin-convention")
}

dependencies {
    api(project(":pycharmctl:api"))
    api(libs.serialization.json)
    api(libs.http4k.core)
}
