plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.aboutLibraries) apply false
}

tasks.register("cleanAppData") {
    group = "cleanup"
    description = "Cleans local application data (database, temp files) for Desktop JVM"
    
    doLast {
        val appData = System.getenv("APPDATA")?.let { java.io.File(it, "GeeFlow") }
            ?: java.io.File(System.getProperty("user.home"), ".geeflow")
        
        if (appData.exists()) {
            println("Deleting app data directory: ${appData.absolutePath}")
            appData.deleteRecursively()
        } else {
            println("App data directory not found: ${appData.absolutePath}")
        }

        val dataStoreFile = java.io.File(System.getProperty("java.io.tmpdir"), "geeflow_settings.preferences_pb")
        if (dataStoreFile.exists()) {
            println("Deleting DataStore file: ${dataStoreFile.absolutePath}")
            dataStoreFile.delete()
        } else {
            println("DataStore file not found: ${dataStoreFile.absolutePath}")
        }
    }
}
