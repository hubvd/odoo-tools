rootProject.name = "odoo-tools"
include("odoo")
include("workspace")
include("config")
include("worktree")
include("addons")
include("reachability-metadata")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
