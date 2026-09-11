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
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.filekit.coil)
            implementation(libs.coil.compose)
            implementation(libs.materialKolor)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
        }
        jvmTest.dependencies {
            implementation(libs.compose.uiTest)
            implementation(compose.desktop.currentOs)
        }
    }
}
