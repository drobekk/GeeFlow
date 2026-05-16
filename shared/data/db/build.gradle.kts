plugins {
    id("kmp.library")
    id("kmp.sqldelight")
    id("kmp.koin")
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("app.geeflow.data.db")
        }
    }
}
