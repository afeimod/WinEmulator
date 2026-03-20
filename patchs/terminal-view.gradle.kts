plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.termux.terminal"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
    implementation(project(":termux-shared"))
    implementation("androidx.annotation:annotation:1.7.0")
}
