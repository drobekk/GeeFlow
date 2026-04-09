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
            api(projects.core.domain)
            api(projects.data.brew)
            api(projects.data.device)
            api(projects.domain.user)
            api(projects.domain.device)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
