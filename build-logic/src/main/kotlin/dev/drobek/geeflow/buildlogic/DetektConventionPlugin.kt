package dev.drobek.geeflow.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(target.libs.findPlugin("detekt").get().get().pluginId)
        target.dependencies.add("detektPlugins", target.libs.findLibrary("detekt-formatting").get())

        target.extensions.configure<DetektExtension> {
            parallel = true
            buildUponDefaultConfig = true
            autoCorrect = true
            config.setFrom(target.rootProject.files("config/detekt/detekt.yml"))
        }

        target.tasks.withType<Detekt>().configureEach {
            exclude("**/generated/**", "**/build/**")
            reports {
                html.required.set(true)
                xml.required.set(false)
                txt.required.set(false)
                sarif.required.set(false)
                md.required.set(false)
            }
        }

        target.afterEvaluate {
            val detektAll = target.tasks.named("detekt")
            val analysisTasksOnly = target.tasks.withType<Detekt>().matching { it.name != "detekt" }
            detektAll.configure { dependsOn(analysisTasksOnly) }
        }
    }
}
