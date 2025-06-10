plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.newsliteapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.newsliteapp"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Instal retrofit2
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Instal retrofit2-converter
// OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.1") // Instal okhttp (caching, logging, dll)
    implementation("com.squareup.picasso:picasso:2.71828") // Instal picasso (Menampilkan gambar)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0") // Instal swipe refresh layout
    implementation("androidx.appcompat:appcompat:1.6.1")
}