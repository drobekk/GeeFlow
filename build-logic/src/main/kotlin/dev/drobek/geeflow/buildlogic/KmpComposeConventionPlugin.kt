package dev.drobek.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kmp.library")
        pluginManager.apply(libs.findPlugin("composeMultiplatform").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("composeCompiler").get().get().pluginId)

        extensions.configure<KotlinMultiplatformExtension> {
            configureAndroidLibrary(
                namespace = defaultNamespace(),
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt(),
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt(),
                enableAndroidResources = true,
            )

            sourceSets.apply {
                commonMain.dependencies {
                    implementation(libs.findLibrary("compose-runtime").get())
                    implementation(libs.findLibrary("compose-foundation").get())
                    implementation(libs.findLibrary("compose-material3").get())
                    implementation(libs.findLibrary("compose-ui").get())
                    implementation(libs.findLibrary("compose-components-resources").get())
                    implementation(libs.findLibrary("androidx-lifecycle-viewmodelCompose").get())
                    implementation(libs.findLibrary("androidx-lifecycle-runtimeCompose").get())
                }
                androidMain.dependencies {
                    implementation(libs.findLibrary("androidx-compose-ui-tooling").get())
                    implementation(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                }
            }
        }
    }
}
