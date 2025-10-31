plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
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


    implementation("androidx.ui:ui-framework:0.1.0-dev03")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

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