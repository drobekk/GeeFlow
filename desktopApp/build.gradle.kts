import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(projects.shared.app)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    
    implementation(libs.blue.falcon.engine.windows)
    implementation(libs.blue.falcon.engine.macos.jvm)
    
    implementation(libs.koin.core)
    implementation(libs.koin.annotations)
    implementation(libs.filekit.core)
}

val osName: String = System.getProperty("os.name")
val osArch: String = System.getProperty("os.arch")
val isWindows = osName.startsWith("Windows", ignoreCase = true)
val isArm64 = osArch == "aarch64" || osArch.contains("arm", ignoreCase = true)

val nativeArch = when {
    isWindows -> if (isArm64) "windows-arm64" else "windows-x64"
    else -> null
}

val nativeLibPath = nativeArch?.let { File(project.projectDir, "resources/$it").absolutePath }

tasks.withType<JavaExec>().configureEach {
    nativeLibPath?.let { systemProperty("java.library.path", it) }
}

compose.desktop {
    application {
        mainClass = "app.geeflow.MainKt"

        if (isWindows) {
            val arch = if (isArm64) "windows-arm64" else "windows-x64"
            jvmArgs += "-Djava.library.path=\$APPDIR/resources/$arch"
        }

        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "GeeFlow"
            packageVersion = (project.findProperty("appVersionName") as? String) ?: libs.versions.appVersion.get()
            
            modules("java.sql", "jdk.unsupported")
            
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "GeeFlow"
                shortcut = true
                dirChooser = true
                upgradeUuid = "1aed0245-316c-4b4d-89ad-46b5ce0b6a13"
            }

            macOS {
                iconFile.set(project.file("icon.icns"))
                bundleID = "app.geeflow"
            }
        }
    }
}
