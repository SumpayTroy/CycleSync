plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.dagger.hilt)
    kotlin("kapt")
}
val openWeatherApiKey: String = project.findProperty("OPENWEATHER_API_KEY") as? String ?: System.getenv("OPENWEATHER_API_KEY") ?: ""

android {
    namespace = "com.bsit.cyclesync"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bsit.cyclesync"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENWEATHER_API_KEY","\"$openWeatherApiKey\"")
        
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

}


dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Compose Material Icons
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Maps + Location
    implementation(libs.google.maps.compose)
    implementation(libs.google.maps.services)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:android-maps-utils:2.4.0")

    // Runtime Permission
    implementation(libs.accompanist.permission)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.google.coroutine)

    // Compose Navigation
    implementation(libs.compose.navigation)

    // Firebase (use BOM to align versions)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Hilt
    implementation(libs.hilt.viewmodel.compose)
    implementation(libs.android.hilt)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    kapt(libs.android.hilt.compiler)
    implementation(libs.androidx.compose.hilt.navigation)

    // UI + Support
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.vision.internal.vkp)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")

    // DataStore
    implementation(libs.androidx.datastore)

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")


    // Weather API support
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Animations
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230227")




}
