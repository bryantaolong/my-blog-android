plugins {
    alias(libs.plugins.android.application)
    // 如果您仍然使用Kotlin来编写部分逻辑，可以保留此行，否则可以移除
    // id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.my_blog_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.my_blog_android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 对于传统视图，通常不需要 vectorDrawables 配置，除非您明确使用矢量图
        // vectorDrawables {
        //     useSupportLibrary = true
        // }
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
        // 确保使用Java 11
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // 移除 kotlinOptions 和 buildFeatures.compose
    // 移除 composeOptions
    // 移除 packaging
}

dependencies {
    // 核心 AndroidX 依赖
    implementation(libs.appcompat)
    implementation(libs.material) // Material Design 组件
    implementation(libs.activity) // Activity 基础库
    implementation(libs.constraintlayout) // 推荐的布局方式


    // --- 推荐的网络请求库 (Java) ---
    // Retrofit: 类型安全的 HTTP 客户端
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // 用于 JSON 转换

    // OkHttp: Retrofit 底层依赖
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // 用于网络请求日志

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // 用于生成必要的代码

    // --- 测试依赖 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}