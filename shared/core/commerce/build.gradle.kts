plugins {
    id("kmp.compose")
    id("kmp.koin")
    alias(libs.plugins.koin.compiler)
}

val mobileSrcDir = "src/mobileMain/kotlin"

koinCompiler {
    compileSafety = false
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.geeflow.commerce.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        androidMain.get().kotlin.srcDir(mobileSrcDir)
        iosMain.get().kotlin.srcDir(mobileSrcDir)

        androidMain.dependencies {
            implementation(libs.inapppurchase)
        }
        iosMain.dependencies {
            implementation(libs.inapppurchase)
        }

        commonMain.dependencies {
            implementation(projects.shared.core.ui)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

