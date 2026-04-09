rootProject.name = "GeeFlow"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core:domain")
include(":core:navigation")
include(":core:presentation")
include(":core:ui")
include(":data:db")
include(":data:brew")
include(":data:user")
include(":data:device")
include(":domain:user")
include(":domain:device")
include(":domain:brew")
include(":permissions")
include(":feature:intro")
include(":feature:device")
include(":composeApp")
include(":androidApp")
