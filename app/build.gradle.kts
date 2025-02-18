plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id ("kotlin-kapt")
}

android {
    namespace = "com.example.payroll"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.payroll"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true

    }
}

dependencies {
    implementation (libs.androidx.material)
    implementation(libs.accompanist.permissions)
    //noinspection GradleDependency
    implementation (libs.androidx.runtime.livedata)
    implementation (libs.play.services.location)
    implementation( libs.androidx.work.runtime.ktx.v281)
    implementation( libs.play.services.location.v2101)
    implementation(libs.coil.kt.coil.compose)
    // Retrofit
    implementation (libs.retrofit)
// Gson converter
    implementation (libs.converter.gson)
    implementation(libs.androidx.constraintlayout.compose.android)
    implementation(libs.androidx.compose.material)
    val room_version = "2.6.1"
    implementation (libs.androidx.room.runtime)
    implementation (libs.androidx.room.ktx) // Add this line for Kotlin extensions
    annotationProcessor (libs.androidx.room.compiler)
    kapt("androidx.room:room-compiler:$room_version")

    // To use Kotlin annotation processing tool (kapt)
//    kapt("androidx.room:room-compiler:$room_version")
    // To use Kotlin Symbol Processing (KSP)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}