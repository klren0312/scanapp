plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
        }
    }
    
    // HarmonyOS target (ohosArm64) - using Kotlin/Native
    // Note: HarmonyOS support may require additional configuration
}

android {
    compileSdk = 34
    namespace = "com.example.scanapp.shared"
    defaultConfig {
        minSdk = 26 // Android 8.0
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

ksp {
    arg("moduleId", "shared")
}

sqldelight {
    databases {
        create("ScanAppDatabase") {
            packageName.set("com.example.scanapp.db")
        }
    }
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.tencent.kuikly:core-ksp:2.0.0")
    // Kuikly core dependency
    commonMainImplementation("com.tencent.kuikly:core:2.0.0")
    // SQLDelight coroutines extensions
    commonMainImplementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
    // Test dependencies
    commonTestImplementation("kotlin-test")
    commonTestImplementation("kotlin-test-common")
    commonTestImplementation("kotlin-test-annotations-common")
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    commonTestImplementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
}