plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.termux.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-proguard-rules.pro")
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

    libraryVariants.all {
        generateBuildConfigProvider.get().enabled = false
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    compileOnly("com.google.code.gson:gson:2.10.1")
}
