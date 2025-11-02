// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) version "8.13.0" apply false
    alias(libs.plugins.kotlin.android) version "2.2.10" apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("com.android.library") version "8.13.0" apply false
    id("app.cash.sqldelight") version "2.1.0" apply false
}