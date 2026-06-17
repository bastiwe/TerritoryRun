plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun mapTilerApiKey(): String {
    val raw = localProperties.getProperty("MAPTILER_API_KEY", "").trim()
    return raw
        .substringAfterLast("=")
        .substringAfterLast("key=")
        .trim()
}

fun routingBaseUrl(): String =
    localProperties.getProperty("ROUTING_BASE_URL", "").trim()

android {
    namespace = "com.example.areawalker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.areawalker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "MAPTILER_API_KEY",
            "\"${mapTilerApiKey()}\""
        )
        buildConfigField(
            "String",
            "ROUTING_BASE_URL",
            "\"${routingBaseUrl()}\""
        )
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

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.google.play.services.games.v2)
    implementation(libs.google.play.services.location)
    implementation(libs.maplibre.android)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
