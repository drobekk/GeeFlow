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
            api(projects.shared.data.brew)
            api(projects.shared.data.device)
            api(projects.shared.domain.user)
            api(projects.shared.domain.device)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
