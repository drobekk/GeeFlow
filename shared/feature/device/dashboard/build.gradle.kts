plugins {
    id("kmp.feature")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.koin.compiler)
}

koinCompiler {
    compileSafety = false
}


kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.presentation)
            implementation(projects.shared.core.ui)
            implementation(projects.shared.domain.device)
            implementation(projects.shared.domain.brew)
            implementation(projects.shared.permissions)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptiveLayout)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.reorderable)
            implementation(libs.kermit)
            implementation(libs.kotlinx.datetime)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.materialKolor)
        }
    }
}

val screenshotCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")

tasks.register<JavaExec>("generateStoreScreenshots") {
    group = "documentation"
    description = "Render store screenshots from Compose previews."
    dependsOn(screenshotCompilation.compileTaskProvider)
    classpath = files(screenshotCompilation.output.allOutputs, screenshotCompilation.runtimeDependencyFiles)
    mainClass.set("app.geeflow.presentation.feature.device.dashboard.StoreScreenshotGeneratorKt")
    args(rootProject.layout.projectDirectory.dir("docs/screenshots").asFile.absolutePath)
}
