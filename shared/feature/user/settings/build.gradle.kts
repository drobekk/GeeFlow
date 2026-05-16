plugins {
    id("kmp.feature")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

tasks.configureEach {
    if (name.contains("generateComposeResClass", ignoreCase = true) ||
        name.contains("copyNonXmlValueResources", ignoreCase = true)
    ) {
        dependsOn(":shared:app:exportLibraryDefinitions")
    }
}

koinCompiler {
    compileSafety = false
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.presentation)
            implementation(projects.shared.core.ui)
            implementation(projects.shared.domain.brew)
            implementation(projects.shared.domain.user)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.aboutLibraries.compose.m3)
            implementation(libs.aboutLibraries.core)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.compose.colorpicker)
            implementation(libs.kermit)
        }
    }
}
