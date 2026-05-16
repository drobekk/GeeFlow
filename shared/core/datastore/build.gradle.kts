plugins {
    id("kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.datastore.preferences)
        }
    }
}
