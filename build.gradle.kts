plugins {
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
    dependencies {
        classpath("com.tencent.kuikly-open:core-ksp:2.0.0-2.0.21")
        // Kotlin Gradle plugin for JVM/Android/Multiplatform modules
        classpath(kotlin("gradle-plugin", version = "2.0.21"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}
