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
            implementation(projects.permissions)
            implementation(projects.domain.user)
            implementation(projects.core.domain)
            implementation(projects.domain.device)
            implementation(projects.domain.brew)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.qrCode.scanner)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.reorderable)
            implementation(libs.kermit)
            implementation(libs.compose.uiToolingPreview)
        }
        jvmMain.dependencies {
            implementation(libs.compose.material3.adaptive)
        }
    }
}
