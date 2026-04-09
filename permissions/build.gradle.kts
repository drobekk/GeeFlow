plugins {
    id("kmp.compose")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            api(libs.moko.permissions)
            api(libs.moko.permissions.bluetooth)
            api(libs.moko.permissions.compose)
        }
        iosMain.dependencies {
            api(libs.moko.permissions)
            api(libs.moko.permissions.bluetooth)
            api(libs.moko.permissions.compose)
        }
    }
}
