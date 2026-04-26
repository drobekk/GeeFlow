package app.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("detekt")
        pluginManager.apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("androidKmpLibrary").get().get().pluginId)

        extensions.configure<KotlinMultiplatformExtension> {
            configureAndroidLibrary(
                namespace = defaultNamespace(),
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt(),
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt(),
            )

            iosArm64()
            iosSimulatorArm64()

            jvm()

            sourceSets.apply {
                commonTest.dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
