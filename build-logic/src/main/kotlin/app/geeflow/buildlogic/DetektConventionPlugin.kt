package app.geeflow.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(target.libs.findPlugin("detekt").get().get().pluginId)
        target.dependencies.add("detektPlugins", target.libs.findLibrary("detekt-formatting").get())

        target.extensions.configure<DetektExtension> {
            config.setFrom(target.rootProject.files("config/detekt/detekt.yml"))
        }

        target.tasks.withType<Detekt>().configureEach {
            multiPlatformEnabled.set(true)
            parallel.set(true)
            exclude("**/generated/**", "**/build/**")
            exclude { it.file.absolutePath.contains("/build/") || it.file.absolutePath.contains("\\build\\") }
            buildUponDefaultConfig.set(true)
            reports {
                checkstyle.required.set(true)
                html.required.set(true)
                sarif.required.set(true)
                markdown.required.set(true)
            }
        }

        target.afterEvaluate {
            val detektAll = target.tasks.named("detekt")
            val analysisTasksOnly = target.tasks.withType<Detekt>().matching { it.name != "detekt" }
            detektAll.configure { dependsOn(analysisTasksOnly) }
        }
    }
}
