plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") // 追加
}

android {
    namespace = "com.example.androidtodo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.androidtodo"
        minSdk = 27
        targetSdk = 34
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
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_17 // 修正
        targetCompatibility = JavaVersion.VERSION_17 // 修正

    }
    kotlinOptions {
        //  jvmTarget = "1.8"
        jvmTarget = "17" // 修正
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        //kotlinCompilerExtensionVersion = "1.5.1"
        kotlinCompilerExtensionVersion = "1.5.11" // 修正
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Room(データベース)ライブラリ
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:${roomVersion}")
    // kspが赤くなる場合、一旦、KSPの一行をコメントアウトし、「Sync Now」ボタン押下。その後にコメントイン(コメント状態解除)して、再び「Sync Now」ボタン押下
    ksp("androidx.room:room-compiler:${roomVersion}")
    implementation("androidx.room:room-ktx:${roomVersion}")

    // Navigation(画面遷移)ライブラリ
    val navVersion = "2.7.7"
    implementation ("androidx.navigation:navigation-compose:${navVersion}")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}