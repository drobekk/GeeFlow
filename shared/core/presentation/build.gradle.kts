plugins {
    id("kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.domain)
            api(projects.shared.core.navigation)
            api(projects.shared.core.ui)
            api(compose.components.resources)
        }
    }
}
