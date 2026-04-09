package dev.drobek.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpSqlDelightConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.findPlugin("sqldelight").get().get().pluginId)

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.apply {
                        androidMain.dependencies {
                            implementation(libs.findLibrary("sqldelight-android-driver").get())
                        }
                        nativeMain.dependencies {
                            implementation(libs.findLibrary("sqldelight-native-driver").get())
                        }
                        jvmMain.dependencies {
                            implementation(libs.findLibrary("sqldelight-jvm-driver").get())
                        }
                    }
                }
            }
        }
    }
}
