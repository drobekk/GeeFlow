package dev.drobek.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpKoinConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("koin-core").get())
                        implementation(libs.findLibrary("koin-annotations").get())
                    }
                }
            }
        }
    }
}
