rootProject.name = "odoo-tools"
include("odoo")
include("workspace")
include("config")
include("worktree")
include("addons")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
