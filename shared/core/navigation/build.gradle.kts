plugins {
    id("kmp.compose")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.navigation3.ui)
            api(libs.androidx.navigationevent.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
