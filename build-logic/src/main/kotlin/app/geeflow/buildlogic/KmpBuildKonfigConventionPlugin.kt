package app.geeflow.buildlogic

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

class KmpBuildKonfigConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.findPlugin("buildKonfig").get().get().pluginId)

        val appVersion = libs.findVersion("appVersion").get().requiredVersion
        val appVersionCode = libs.findVersion("appVersionCode").get().requiredVersion
        val appPackageName = libs.findVersion("appPackageName").get().requiredVersion

        extensions.configure<BuildKonfigExtension> {
            packageName.set(appPackageName)
            exposeObjectWithName.set("BuildKonfig")
            defaultConfigs {
                buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", appVersion)
                buildConfigField(FieldSpec.Type.STRING, "APP_VERSION_CODE", appVersionCode)
                buildConfigField(FieldSpec.Type.STRING, "PACKAGE_NAME", appPackageName)
            }
        }

        tasks.configureEach {
            if (name.startsWith("prepareAndroid") && name.endsWith("ArtProfile")) {
                dependsOn("generateBuildKonfig")
            }
        }

        val root = rootProject
        // Resolve to String at configuration time — Project cannot be serialized by config cache
        val xcConfigFilePath = root.layout.projectDirectory
            .file("iosApp/Configuration/Version.xcconfig").asFile.absolutePath
        val xcConfigTask = if (root.tasks.names.contains("generateVersionXcconfig")) {
            root.tasks.named("generateVersionXcconfig")
        } else {
            root.tasks.register("generateVersionXcconfig") {
                doLast {
                    File(xcConfigFilePath).writeText(
                        "// Generated — do not edit. Change app-version in libs.versions.toml.\n" +
                            "MARKETING_VERSION = $appVersion\n" +
                            "CURRENT_PROJECT_VERSION = $appVersionCode\n",
                    )
                }
            }
        }
        root.subprojects {
            tasks.configureEach {
                if (name == "preBuild") dependsOn(xcConfigTask)
            }
        }
    }
}
