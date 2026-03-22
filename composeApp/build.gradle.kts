import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        namespace = "dev.drobek.geeflow"
        androidResources { enable = true }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.binaries.all {
            linkerOpts("-lsqlite3", "-lz")
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.qrCode.scanner)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.blue.falcon)
            implementation(libs.kermit)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.jvm.driver)
            implementation(libs.compose.material3.adaptive)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
            api(libs.moko.permissions)
            api(libs.moko.permissions.bluetooth)
            api(libs.moko.permissions.compose)
        }
        iosMain.dependencies {
            api(libs.moko.permissions)
            api(libs.moko.permissions.bluetooth)
            api(libs.moko.permissions.compose)
        }
        nativeMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

dependencies {
    "androidRuntimeClasspath"(libs.androidx.compose.ui.tooling)
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("dev.drobek.geeflow")
        }
    }
}

koinCompiler {
    userLogs = true
    debugLogs = false
    compileSafety = true
}

compose.desktop {
    application {
        mainClass = "dev.drobek.geeflow.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.drobek.geeflow"
            packageVersion = "1.0.0"
        }
    }
}
