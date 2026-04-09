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
            api(projects.data.db)
            api(projects.data.brew)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.blue.falcon)
            implementation(libs.kermit)
        }
    }
}
