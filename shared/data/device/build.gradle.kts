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
            packageName.set("app.geeflow.data.device.db")
            dependency(project(":shared:data:user"))
        }
    }
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain.dependencies {
            api(projects.shared.core.domain)
            api(projects.shared.data.user)
            api(projects.shared.data.brew)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.blue.falcon.core)
            implementation(libs.kermit)
        }
    }
}
