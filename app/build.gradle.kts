plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("app.cash.sqldelight")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {

    namespace = "com.example.easyeats"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.easyeats"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kotlin {
        jvmToolchain(17)
    }



}



dependencies {
    // Dagger hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.ui:ui-framework:0.1.0-dev03")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // SQLDelight
    implementation("app.cash.sqldelight:android-driver:2.1.0")


    // Lifecycle
    implementation(libs.androidx.activity.compose.v140)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")


    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Coil for Compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Core
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat.v170)
    implementation(libs.material.v1110)

    // Networking & JSON
    implementation(libs.okhttp)
    implementation(libs.gson)

    // UI
    implementation(libs.androidx.recyclerview)
//    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.coil)

    // Tests (optional)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Example of a standard test dependency
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Example of a standard test dependency
}

