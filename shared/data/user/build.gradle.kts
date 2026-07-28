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
            packageName.set("app.geeflow.data.user.db")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(projects.shared.core.datastore)
        }
    }
}
