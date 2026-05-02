import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kmp.feature")
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.aboutLibraries)
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
            api(projects.feature.device.list)
            api(projects.feature.device.add)
            api(projects.feature.device.dashboard)
            api(projects.feature.device.settings)
            api(projects.feature.user.settings)
            api(projects.feature.user.list)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.kermit)
            implementation(libs.coil.compose)
            implementation(libs.filekit.coil)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.blue.falcon.engine.windows)
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


val nativeLibPath: String? = (project.findProperty("nativeArch") as String?)
    ?.let { "${project.projectDir}/resources/$it" }

tasks.withType<JavaExec>().configureEach {
    nativeLibPath?.let { systemProperty("java.library.path", it) }
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

compose.desktop {
    application {
        mainClass = "app.geeflow.MainKt"

        nativeLibPath?.let { jvmArgs += "-Djava.library.path=$it" }

        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = libs.versions.appPackageName.get()
            packageVersion = libs.versions.appVersion.get()
        }
    }
}
