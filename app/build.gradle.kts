import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// AdMob 앱 ID/배너 광고 단위 ID는 저장소에 노출하지 않도록 local.properties에서 읽는다 (VCS 제외 파일).
// 값이 없으면 Google 공식 테스트 ID로 대체해 빌드는 깨지지 않게 하되, 그대로 릴리스하면 실제 광고가
// 나오지 않으므로 경고를 남긴다. debug 빌드는 본인 광고에 대한 무효 트래픽을 막기 위해 값이 있어도
// 항상 테스트 ID를 쓴다.
val adMobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val adMobTestBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val adMobTestNativeUnitId = "ca-app-pub-3940256099942544/2247696110"
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun adMobProperty(key: String, testValue: String): String =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: testValue.also {
        logger.warn("WARNING: $key is missing in local.properties - release builds will use the AdMob TEST id.")
    }
val adMobAppId: String = adMobProperty("tossWatch.adMobAppId", adMobTestAppId)
val adMobBannerUnitId: String = adMobProperty("tossWatch.adMobBannerUnitId", adMobTestBannerUnitId)
val adMobNativeUnitId: String = adMobProperty("tossWatch.adMobNativeUnitId", adMobTestNativeUnitId)

android {
    namespace = "dev.comon.toss_watch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.comon.toss_watch"
        minSdk = 26
        targetSdk = 37
        versionCode = 11
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["adMobAppId"] = adMobTestAppId
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$adMobTestBannerUnitId\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$adMobTestNativeUnitId\"")
        }
        release {
            manifestPlaceholders["adMobAppId"] = adMobAppId
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$adMobBannerUnitId\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$adMobNativeUnitId\"")
            optimization {
                enable = true
            }
        }
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
    implementation(project(":feature:auth"))
    implementation(project(":feature:alarm"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:setting"))
    implementation(project(":feature:tosskey"))

    implementation(libs.hilt.android)
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.ads)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mlkit.barcode.scanning)
    androidTestImplementation(libs.zxing.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
