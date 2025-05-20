// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    extra["kotlin_version"] = "2.1.20"

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("com.google.gms:google-services:4.3.15")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
        google()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
