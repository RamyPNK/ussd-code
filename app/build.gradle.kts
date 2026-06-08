plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ussd.gateway"
    compileSdk = 35 // قم بتغيير هذا الرقم إلى 35

    defaultConfig {
        applicationId = "com.ussd.gateway"
        minSdk = 23
        targetSdk = 35 // يفضل تحديث هذا الرقم أيضاً ليطابق ما قبله
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    // مكتبات غير موجودة في الـ TOML (ستبقى لها تحذيرات بسيطة وهذا طبيعي)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.compose.foundation:foundation")

    // مكتبات تم تحويلها لاستخدام Version Catalog
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}