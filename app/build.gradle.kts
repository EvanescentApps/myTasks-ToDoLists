plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

extra["kotlin_version"] = "2.3.21"


android {
    namespace = "com.evanescent.mytasks"
    compileSdk = 37


    testOptions {
        unitTests {
            isReturnDefaultValues = true // Needed for Mockito to return default values for mocks
            isIncludeAndroidResources = true // For Robolectric if you use it (not strictly needed for basic JUnit/Mockito)
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "todolistapp"
            keyPassword = "@OmegaNano4497"
            storePassword = "@OmegaNano4497"
            storeFile = file("../todolistapp.jks")
        }
        create("release") {
            keyAlias = "todolistapp"
            keyPassword = "@OmegaNano4497"
            storePassword = "@OmegaNano4497"
            storeFile = file("../todolistapp.jks")
        }
    }

    defaultConfig {
        applicationId = "com.evanescent.mytasks"
        minSdk = 23
        targetSdk = 37
        versionCode = 16
        versionName = "2.1"
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    compileSdkMinor = 0
    buildToolsVersion = "37.0.0"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    // Unit Testing (Local JVM Tests)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core) // Or latest version
    testImplementation(libs.mockito.kotlin) // If using Kotlin for mocks
    testImplementation(libs.core.testing) // For LiveData testing
    testImplementation(libs.kotlinx.coroutines.test) // For Coroutine testing
    testImplementation(libs.truth) // Optional: More readable assertions

    // Instrumented Testing (On-device Tests)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.espresso.contrib) // For RecyclerView testing
    androidTestImplementation(libs.mockito.android) // If need mocks in instrumented tests
    androidTestImplementation(libs.core.testing) // For LiveData testing in instrumented tests
    androidTestImplementation(libs.truth) // Optional: More readable assertions


    implementation(libs.firebase.bom)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.sparkbutton)
    implementation(libs.lottie)
    implementation(libs.timber)

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.preference.ktx)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)
    implementation(libs.datastore.preferences)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.pronotelib)

    // Room
    val roomVersion = "2.8.4"
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Jetpack Compose
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)


    implementation(libs.activity.ktx) // for by viewModels()
    implementation(libs.fragment.ktx)
    implementation(libs.material3)
/*
    implementation("androidx.compose.material3:material3-pullrefresh:1.2.0-alpha11")
*/

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

repositories {
    google()
    mavenCentral()
}
