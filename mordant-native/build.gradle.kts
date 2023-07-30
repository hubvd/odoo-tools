plugins {
    id("kotlin-convention")
}

dependencies {
    compileOnly(libs.graalvm.svm)
}
