package app.geeflow.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun KotlinMultiplatformExtension.configureAndroidLibrary(
    namespace: String,
    compileSdk: Int,
    minSdk: Int,
    enableAndroidResources: Boolean = false,
) {
    val ext = (this as ExtensionAware).extensions
        .getByName("androidLibrary") as KotlinMultiplatformAndroidLibraryExtension

    ext.namespace = namespace
    ext.compileSdk = compileSdk
    ext.minSdk = minSdk
    ext.androidResources.enable = enableAndroidResources
}
