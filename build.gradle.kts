plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.8" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

buildscript {
    extra["kotlin_version"] = "2.3.21"

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
        google()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.google.services)
        // classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
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
