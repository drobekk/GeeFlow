import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kmp.feature")
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

kotlin {
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

    sourceSets {
        commonMain.dependencies {
            api(projects.core.navigation)
            api(projects.core.presentation)
            api(projects.core.ui)
            api(projects.data.db)
            api(projects.data.brew)
            api(projects.data.user)
            api(projects.data.device)
            api(projects.domain.user)
            api(projects.domain.device)
            api(projects.domain.brew)
            api(projects.feature.intro)
            api(projects.feature.device)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.blue.falcon)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.compose.material3.adaptive)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

dependencies {
    "androidRuntimeClasspath"(libs.androidx.compose.ui.tooling)
}

koinCompiler {
    userLogs = true
    debugLogs = false
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
