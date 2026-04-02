@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Kotlin 2.0+ way to enable Compose; replaces the old compose-compiler extension
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.saurabh.sonicaudiorecorder"
    //noinspection GradleDependency
    compileSdk = 35

    defaultConfig {
        applicationId = "com.saurabh.sonicaudiorecorder"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 8
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Enable minification and resource shrinking for production builds
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 1. Native debug symbols for Play Console crash analysis (for C/C++ in dependencies like Media3)
            ndk {
                debugSymbolLevel = "FULL"
            }
            
            // 2. Ensure R8/ProGuard mapping files are generated and retained
            // These are normally included in the App Bundle (.aab) automatically.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Modern way to configure Kotlin options in Gradle
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            // Adding compiler args for Compose metrics and performance reports
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                        project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"
            )
        }
    }

    buildFeatures {
        // Required for AGP Compose integration (Layout Inspector, etc.)
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            // Standard exclusions to prevent merge conflicts with Hilt/Coroutine libraries
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }
}

// KSP Configuration for Room and Hilt
ksp {
    // Room: Generate native Kotlin code instead of Java for better performance with Kotlin 2.x
    arg("room.generateKotlin", "true")
}

dependencies {
    // Core AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Jetpack Compose (using BOM to ensure version compatibility across all compose artifacts)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Room with KSP
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Hilt with KSP (replacing KAPT for faster builds)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Navigation & ViewModel
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Media & Storage
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("fixResourceName") {
    doLast {
        val oldFile = file("src/main/res/drawable/ic_app_icon.png.png")
        val newFile = file("src/main/res/drawable/ic_app_icon.png")
        if (oldFile.exists()) {
            if (oldFile.renameTo(newFile)) {
                println("Successfully renamed ic_app_icon.png.png to ic_app_icon.png")
            } else {
                throw GradleException("Failed to rename file")
            }
        } else {
            println("File ic_app_icon.png.png does not exist")
        }
    }
}
