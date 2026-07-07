plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
        val androidUnitTest by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
                implementation("androidx.core:core-ktx:1.7.0")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.tencent.kuikly-open:core-ksp:2.0.0-2.0.21")
    // Kuikly core dependency
    commonMainImplementation("com.tencent.kuikly-open:core:2.0.0-2.0.21")
    commonMainImplementation("com.tencent.kuikly-open:core-annotations:2.0.0-2.0.21")
    // SQLDelight coroutines extensions
    commonMainImplementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
    // kotlinx serialization
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // Test dependencies
    commonTestImplementation(kotlin("test"))
    commonTestImplementation(kotlin("test-common"))
    commonTestImplementation(kotlin("test-annotations-common"))
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    commonTestImplementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
}

