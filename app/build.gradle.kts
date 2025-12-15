plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"

}

extra["kotlin_version"] = "2.1.20"


android {
    namespace = "com.electro.todolist"
    compileSdk = 36


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
            storeFile = file("/home/ecocain/StudioProjects/ToDoList/todolistapp.jks")
        }
        create("release") {
            keyAlias = "todolistapp"
            keyPassword = "@OmegaNano4497"
            storePassword = "@OmegaNano4497"
            storeFile = file("/home/ecocain/StudioProjects/ToDoList/todolistapp.jks")
        }
    }

    defaultConfig {
        applicationId = "com.electro.todolist"
        minSdk = 23
        targetSdk = 36
        versionCode = 15
        versionName = "2.0 Reborn"
        multiDexEnabled = true
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.8.1" // Compatible avec Kotlin 2.1.20
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // Unit Testing (Local JVM Tests)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.20.0") // Or latest version
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0") // If using Kotlin for mocks
    testImplementation("androidx.arch.core:core-testing:2.2.0") // For LiveData testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") // For Coroutine testing
    testImplementation("com.google.truth:truth:1.4.5") // Optional: More readable assertions

    // Instrumented Testing (On-device Tests)
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0") // For RecyclerView testing
    androidTestImplementation("org.mockito:mockito-android:5.20.0") // If you need mocks in instrumented tests
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0") // For LiveData testing in instrumented tests
    androidTestImplementation("com.google.truth:truth:1.4.5") // Optional: More readable assertions


    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx:22.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("com.github.varunest:sparkbutton:1.0.6")
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("com.github.LelouBil:PronoteLib:0.4.2")

    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.8.1")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.1")


    implementation("androidx.activity:activity-ktx:1.10.1") // for by viewModels()
    implementation("androidx.compose.material3:material3:1.3.2")
/*
    implementation("androidx.compose.material3:material3-pullrefresh:1.2.0-alpha11")
*/

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

repositories {
    google()
    mavenCentral()
}
