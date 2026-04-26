package app.geeflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("kmp.compose")
            pluginManager.apply("kmp.koin")

            dependencies.add("commonMainImplementation", libs.findLibrary("koin-compose").get())
            dependencies.add("commonMainImplementation", libs.findLibrary("koin-compose-viewmodel").get())
            dependencies.add("commonMainImplementation", libs.findLibrary("koin-compose-viewmodel-navigation").get())
            dependencies.add("commonMainImplementation", libs.findLibrary("jetbrains-navigation3-ui").get())
            dependencies.add("commonMainImplementation", libs.findLibrary("jetbrains-lifecycle-viewmodelNavigation3").get())
            dependencies.add("commonMainImplementation", libs.findLibrary("kotlinx-serialization-json").get())
        }
    }
}
