plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.sqldelight.gradlePlugin)
    implementation(libs.buildKonfig.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "kmp.library"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "kmp.compose"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "kmp.feature"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpFeatureConventionPlugin"
        }
        register("kmpKoin") {
            id = "kmp.koin"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpKoinConventionPlugin"
        }
        register("kmpSqldelight") {
            id = "kmp.sqldelight"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpSqlDelightConventionPlugin"
        }
        register("kmpAndroid") {
            id = "kmp.android"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpAndroidConventionPlugin"
        }
        register("detekt") {
            id = "detekt"
            implementationClass = "dev.drobek.geeflow.buildlogic.DetektConventionPlugin"
        }
        register("kmpBuildKonfig") {
            id = "kmp.buildkonfig"
            implementationClass = "dev.drobek.geeflow.buildlogic.KmpBuildKonfigConventionPlugin"
        }
    }
}
