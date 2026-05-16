plugins {
    id("kmp.library")
    id("kmp.koin")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.data.db)
            api(projects.shared.data.brew)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.blue.falcon.core)
            implementation(libs.kermit)
        }
    }
}
