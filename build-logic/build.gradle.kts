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
            implementationClass = "app.geeflow.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "kmp.compose"
            implementationClass = "app.geeflow.buildlogic.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "kmp.feature"
            implementationClass = "app.geeflow.buildlogic.KmpFeatureConventionPlugin"
        }
        register("kmpKoin") {
            id = "kmp.koin"
            implementationClass = "app.geeflow.buildlogic.KmpKoinConventionPlugin"
        }
        register("kmpSqldelight") {
            id = "kmp.sqldelight"
            implementationClass = "app.geeflow.buildlogic.KmpSqlDelightConventionPlugin"
        }
        register("kmpAndroid") {
            id = "kmp.android"
            implementationClass = "app.geeflow.buildlogic.KmpAndroidConventionPlugin"
        }
        register("detekt") {
            id = "detekt"
            implementationClass = "app.geeflow.buildlogic.DetektConventionPlugin"
        }
        register("kmpBuildKonfig") {
            id = "kmp.buildkonfig"
            implementationClass = "app.geeflow.buildlogic.KmpBuildKonfigConventionPlugin"
        }
    }
}
