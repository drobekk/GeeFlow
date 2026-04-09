package dev.drobek.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpAndroidConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.findPlugin("androidKmpLibrary").get().get().pluginId)

        extensions.configure<KotlinMultiplatformExtension> {
            configureAndroidLibrary(
                namespace = defaultNamespace(),
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt(),
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt(),
            )
        }
    }
}
