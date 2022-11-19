rootProject.name = "odoo-tools"
include("odoo")
include("workspace")
include("config")
include("worktree")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
