plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ktfmt.gradle) apply false
    alias(libs.plugins.gms) apply false
}

buildscript {
    configurations.getByName("classpath") {
        resolutionStrategy { force(libs.commons.compress.get().toString()) }
    }
}
