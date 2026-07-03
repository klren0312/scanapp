pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}

rootProject.name = "scanapp"

include(":shared")
include(":androidApp")
include(":iosApp")
// HarmonyOS shell module is present as a placeholder to satisfy existing task scaffolding,
// but actual HarmonyOS target integration is deferred in this environment.
