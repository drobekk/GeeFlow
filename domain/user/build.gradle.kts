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
            api(projects.data.user)
            implementation(projects.data.brew)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.filekit.core)
        }
    }
}
