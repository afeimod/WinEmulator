plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.termux.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
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
    implementation(project(":terminal-emulator"))
    implementation(project(":terminal-view"))

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.annotation:annotation:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.window:window:1.1.0")
    implementation("com.google.android.material:material:1.11.0")

    // Guava - use full version for MoreFiles
    implementation("com.google.guava:guava:32.1.3-android")

    // Markwon for markdown
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:recycler:4.6.2")

    // Apache Commons
    implementation("commons-io:commons-io:2.15.1")

    // Hidden API Bypass
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")

    // Termux AM Library
    implementation("com.termux:termux-am-library:v2.0.0")
}
