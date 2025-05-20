plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0-RC"

}

extra["kotlin_version"] = "2.1.20"


android {
    namespace = "com.electro.todolist"
    compileSdk = 35

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
        targetSdk = 35
        versionCode = 15
        versionName = "1.5 Blue"
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
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation("com.github.varunest:sparkbutton:1.0.6")
    implementation("com.airbnb.android:lottie:6.6.6")
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation("com.firebaseui:firebase-ui-auth:9.0.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
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
