package dev.comon.watch_app.di

import dev.comon.watch_app.BuildConfig
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 2-5 `fcm-token/check` 등 워치앱 전용 API가 요구하는 `X-Toss-Watch-Api-Key` 헤더를 부착한다.
 * 값은 서버 `.env`의 `TOSS_WATCH_APP_API_KEY`와 일치해야 하며 워치 빌드에 고정 내장된다.
 */
class WatchApiKeyInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader(HEADER_API_KEY, BuildConfig.TOSS_WATCH_APP_API_KEY)
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_API_KEY = "X-Toss-Watch-Api-Key"
    }
}
