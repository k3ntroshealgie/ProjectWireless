import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}
android {
    namespace = "com.example.campusconnect1"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.campusconnect1"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Read credentials from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        // Cloudinary credentials
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties["CLOUDINARY_CLOUD_NAME"] ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperties["CLOUDINARY_API_KEY"] ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${localProperties["CLOUDINARY_API_SECRET"] ?: ""}\"")
        
        // ImgBB credentials (fallback)
        buildConfigField("String", "IMGBB_API_KEY_1", "\"${localProperties["IMGBB_API_KEY_1"] ?: ""}\"")
        buildConfigField("String", "IMGBB_API_KEY_2", "\"${localProperties["IMGBB_API_KEY_2"] ?: ""}\"")
        buildConfigField("String", "IMGBB_API_KEY_3", "\"${localProperties["IMGBB_API_KEY_3"] ?: ""}\"")
        buildConfigField("String", "IMGBB_ACTIVE_KEY", "\"${localProperties["IMGBB_ACTIVE_KEY"] ?: "IMGBB_API_KEY_1"}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // Mengaktifkan Jetpack Compose
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }
    composeOptions {
        // Sesuaikan versi ini dengan versi Kotlin Anda
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    // Menghindari duplikat file packaging
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
dependencies {
    // Core KTX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose BOM (Bill of Materials)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Navigasi & ViewModel untuk Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Firebase BOM (Mengelola versi library Firebase)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Coroutines (Penting untuk Firebase + Retrofit)
    implementation(libs.kotlinx.coroutines.play.services)
    // Retrofit (Untuk upload gambar imgbb)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    // Cloudinary SDK
    implementation("com.cloudinary:cloudinary-android:2.3.1")
    // Tambahkan ini untuk akses semua ikon (Bookmark, dll)
    implementation("androidx.compose.material:material-icons-extended")
    // Coil (Untuk memuat gambar dari URL)
    implementation(libs.coil.compose)
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}