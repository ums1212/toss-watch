import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// API base URL / 워치앱 전용 API 키(X-Toss-Watch-Api-Key 헤더)는 저장소에 노출하지 않도록 local.properties에서 읽는다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val apiBaseUrl: String = localProperties.getProperty("tossWatch.apiBaseUrl")
    ?.takeIf { it.isNotBlank() }
    ?: error("local.properties에 tossWatch.apiBaseUrl을 설정해야 합니다. ('/'로 끝나야 함)")
val watchApiKey: String = localProperties.getProperty("tossWatch.watchApiKey")
    ?.takeIf { it.isNotBlank() }
    ?: error("local.properties에 tossWatch.watchApiKey를 설정해야 합니다. (서버 .env의 TOSS_WATCH_APP_API_KEY와 일치)")

android {
    namespace = "dev.comon.watch_app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.comon.toss_watch"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "TOSS_WATCH_APP_API_KEY", "\"$watchApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.navigation)
    implementation(libs.play.services.wearable)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.zxing.core)

    // 2-5 fcm-token/check 호출 및 QR 페이로드 직렬화용 최소 네트워크/직렬화 스택.
    // :core:network는 JWT 세션/Tink 암호화 등 폰 로그인 전용 인프라를 함께 끌고 오므로
    // 워치앱은 별도의 경량 Retrofit/OkHttp 구성을 갖는다.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.datastore.preferences)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
