rootProject.name = "odoo-tools"
include("odoo")
include("workspace")
include("config")
include("worktree")
include("addons")
include("actions")
include("reachability-metadata")
include("odoo-client")
include("pycharmctl:api")
include("pycharmctl:client")
include("pycharmctl:plugin")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
