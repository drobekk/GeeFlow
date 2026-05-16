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

        nativeLibPath?.let { jvmArgs += "-Djava.library.path=$it" }

        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = libs.versions.appPackageName.get()
            packageVersion = libs.versions.appVersion.get()
        }
    }
}
