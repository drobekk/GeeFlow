plugins {
    id("kmp.compose")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.navigation3.ui)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
