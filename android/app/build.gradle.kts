plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ch.inf.usi.mindbricks"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ch.inf.usi.mindbricks"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // additional compile options to export the SQL schema
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Lifecycle
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Fragment
    implementation(libs.fragment.ktx)

    // RecyclerView & SwipeRefresh
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    // WorkManager
    implementation(libs.work.runtime)

    // Room (Database)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Charts & Visualization
    implementation(libs.mpandroidchart)

    // Image Loading (Glide)
    implementation(libs.glide.runtime)
    annotationProcessor(libs.glide.compiler)

    // JSON Serialization
    implementation(libs.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}