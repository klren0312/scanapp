plugins {
    kotlin("multiplatform")
}

kotlin {
    ohos {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    sourceSets {
        val ohosMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}