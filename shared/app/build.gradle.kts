plugins {
    id("kmp.feature")
    id("kmp.sqldelight")
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.aboutLibraries)
}

val hasCommerce = rootProject.file("commerce/build.gradle.kts").exists()

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("app.geeflow.app.db")
            dependency(project(":shared:data:user"))
            dependency(project(":shared:data:device"))
            dependency(project(":shared:data:brew"))
        }
    }
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        iosTarget.binaries.all {
            linkerOpts("-lsqlite3", "-lz")
        }
    }


    sourceSets {
        commonMain.get().kotlin.srcDir(
            if (hasCommerce) rootProject.file("commerce/wiring") else file("src/commerceStub/kotlin"),
        )

        commonMain.dependencies {
            api(projects.shared.core.commerce)
            if (hasCommerce) api(project(":commerce"))
            api(projects.shared.core.navigation)
            api(projects.shared.core.presentation)
            api(projects.shared.core.ui)
            api(projects.shared.data.brew)
            api(projects.shared.data.user)
            api(projects.shared.data.device)
            api(projects.shared.domain.user)
            api(projects.shared.domain.device)
            api(projects.shared.domain.brew)
            api(projects.shared.feature.intro)
            api(projects.shared.feature.device.list)
            api(projects.shared.feature.device.add)
            api(projects.shared.feature.device.dashboard)
            api(projects.shared.feature.device.settings)
            api(projects.shared.feature.user.settings)
            api(projects.shared.feature.user.list)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.kermit)
            implementation(libs.coil.compose)
            implementation(libs.filekit.coil)
            implementation(libs.materialKolor)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.blue.falcon.engine.windows)
            implementation(libs.blue.falcon.engine.macos.jvm)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.blue.falcon.engine.android)
        }
        iosMain.dependencies {
            implementation(libs.blue.falcon.engine.ios)
        }
    }
}

dependencies {
    "androidRuntimeClasspath"(libs.androidx.compose.ui.tooling)
}

koinCompiler {
    compileSafety = false
    userLogs = true
    debugLogs = false
}

aboutLibraries {
    export {
        outputPath = file("../feature/user/settings/src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
    }
}


