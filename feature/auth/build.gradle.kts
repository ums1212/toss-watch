plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.comon.toss_watch.feature.auth"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        // 구글 로그인용 웹 클라이언트 ID (GCP 콘솔의 OAuth 2.0 "웹 애플리케이션" 타입).
        // 실제 값은 gradle.properties(로컬) 또는 CI 환경 프로퍼티로 주입한다.
        buildConfigField(
            "String",
            "GOOGLE_SERVER_CLIENT_ID",
            "\"${providers.gradleProperty("tossWatch.googleServerClientId")
                .getOrElse("REPLACE_ME.apps.googleusercontent.com")}\"",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
