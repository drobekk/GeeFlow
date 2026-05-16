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
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.presentation)
            implementation(projects.shared.core.ui)
            implementation(projects.shared.domain.device)
            implementation(projects.shared.permissions)
            implementation(libs.qrCode.scanner)
        }
    }
}
