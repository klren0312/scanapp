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
include(":ohosApp")