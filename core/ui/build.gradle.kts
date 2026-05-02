plugins {
    id("kmp.compose")
}

compose.resources {
    publicResClass = true
    generateResClass = auto
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.filekit.coil)
            implementation(libs.coil.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
        }
    }
}
