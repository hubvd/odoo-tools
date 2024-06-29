import org.graalvm.buildtools.gradle.dsl.GraalVMExtension

plugins {
    id("kotlin-convention")
    id("native-image-convention")
}

dependencies {
    implementation(libs.kotlin.reflect)
    compileOnly(libs.graalvm.svm)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk)
}

project.extensions.configure<GraalVMExtension>("graalvmNative") {
    binaries {
        named("main") {
            buildArgs(
                "--enable-native-access=ALL-UNNAMED",
                "-H:+ForeignAPISupport",
                "-H:+UnlockExperimentalVMOptions",
                "--features=com.github.hubvd.odootools.ffi.libffi.FfiFeature"
            )
            imageName = "ffi-test"
            mainClass = "com.github.hubvd.odootools.ffi.libffi.test.Git_errorKt"
        }
    }
}
