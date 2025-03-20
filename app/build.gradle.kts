plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.codeandcoffee.w_lur"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codeandcoffee.w_lur"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.androidx.compose.bom.v20231001))
    implementation(libs.androidx.navigation.compose)
    // Coil (para cargar imágenes en el futuro)
    implementation(libs.coil.compose)
    implementation(libs.androidx.graphics.core) // O la versión más reciente

    // Permisos
    implementation(libs.accompanist.permissions) // Usa Accompanist
    implementation(libs.ui) // Usa la versión correcta
    implementation(libs.ui.graphics) // Importante para Brush
    implementation(libs.androidx.ui.v164) // Asegúrate de tener la versión más reciente de Compose
    implementation(libs.material3) // Asegúrate de tener la versión más reciente de Material 3
    implementation(libs.androidx.core.ktx.v1120) // Asegúrate de tener la versión más reciente de core-ktx
    implementation(libs.androidx.lifecycle.runtime.ktx.v270) // Asegúrate de tener la versión más reciente de lifecycle-runtime-ktx
    implementation(libs.androidx.activity.compose.v182) // Asegúrate de tener la versión más reciente de activity-compose
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}