plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingEnvironment = mapOf(
    "path" to System.getenv("KEYSTORE_PATH"),
    "storePassword" to System.getenv("KEY_STORE_PASSWORD"),
    "keyPassword" to System.getenv("KEY_PASSWORD"),
    "alias" to System.getenv("ALIAS"),
)

android {
    namespace = "com.atuy.yws1editor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.atuy.yws1editor"
        minSdk = 35
        targetSdk = 37
        versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("VERSION_NAME").orNull ?: "1.0"
    }

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
            isMinifyEnabled = true
            isShrinkResources = true
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
        aidl = true
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
    debugImplementation(libs.androidx.compose.ui.tooling)
}
