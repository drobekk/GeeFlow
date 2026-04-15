plugins {
    id("kmp.feature")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation)
            implementation(projects.core.presentation)
            implementation(projects.core.ui)
            implementation(projects.domain.user)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.aboutLibraries.compose.m3)
            implementation(libs.aboutLibraries.core)
        }
    }
}
