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

include(":shared:core:datastore")
include(":shared:core:domain")
include(":shared:core:navigation")
include(":shared:core:presentation")
include(":shared:core:ui")
include(":shared:data:brew")
include(":shared:data:user")
include(":shared:data:device")
include(":shared:domain:user")
include(":shared:domain:device")
include(":shared:domain:brew")
include(":shared:permissions")
include(":shared:feature:intro")
include(":shared:feature:device:list")
include(":shared:feature:device:add")
include(":shared:feature:device:dashboard")
include(":shared:feature:device:settings")
include(":shared:feature:user:settings")
include(":shared:feature:user:list")
include(":shared:app")
include(":androidApp")
include(":desktopApp")
