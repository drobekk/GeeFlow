plugins {
    id("kmp.feature")
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
            implementation(projects.domain.device)
            implementation(projects.permissions)
            implementation(libs.qrCode.scanner)
        }
    }
}
