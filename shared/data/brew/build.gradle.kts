plugins {
    id("kmp.library")
    id("kmp.sqldelight")
    id("kmp.koin")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("app.geeflow.data.brew.db")
            dependency(project(":shared:data:user"))
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.domain)
            api(projects.shared.data.user)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
