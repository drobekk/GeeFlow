plugins {
    id("kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.navigation)
            api(projects.core.ui)
            api(compose.components.resources)
        }
    }
}
