plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.atuy.yweditor"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.atuy.yweditor"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val signingEnvironment = mapOf(
        "path" to System.getenv("KEYSTORE_PATH"),
        "storePassword" to System.getenv("KEY_STORE_PASSWORD"),
        "keyPassword" to System.getenv("KEY_PASSWORD"),
        "alias" to System.getenv("ALIAS"),
    )
    val releaseSigningConfig = if (signingEnvironment.values.all { !it.isNullOrBlank() }) {
        signingConfigs.create("release") {
            storeFile = file(signingEnvironment.getValue("path")!!)
            storePassword = signingEnvironment.getValue("storePassword")
            keyPassword = signingEnvironment.getValue("keyPassword")
            keyAlias = signingEnvironment.getValue("alias")
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            releaseSigningConfig?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
