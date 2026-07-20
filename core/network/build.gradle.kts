import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// API base URL은 저장소에 노출하지 않도록 local.properties에서 읽는다 (VCS 제외 파일).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val apiBaseUrl: String = localProperties.getProperty("tossWatch.apiBaseUrl")
    ?: error("local.properties에 tossWatch.apiBaseUrl을 설정해야 합니다. ('/'로 끝나야 함)")

android {
    namespace = "dev.comon.toss_watch.core.network"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:datastore"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    api(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}
