plugins {
    id("kmp.library")
    id("kmp.koin")
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.domain)
            api(projects.shared.data.user)
            implementation(projects.shared.data.brew)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.filekit.core)
        }
    }
}
