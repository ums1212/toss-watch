package dev.comon.watch_app.data.remote

import dev.comon.watch_app.data.remote.dto.FcmTokenCheckRequest
import dev.comon.watch_app.data.remote.dto.FcmTokenCheckResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface WatchApi {

    /**
     * 2-5. 워치 FCM 토큰 등록 여부 확인 (워치앱 전용, JWT 불필요).
     * 로그인 세션 없이 로컬에 발급받은 FCM 토큰이 서버 기준으로 등록됐는지만 확인한다.
     * `X-Toss-Watch-Api-Key` 헤더는 [dev.comon.watch_app.di.WatchApiKeyInterceptor]가 부착한다.
     */
    @POST("v1/toss-watch/fcm-token/check/")
    suspend fun checkFcmToken(
        @Body body: FcmTokenCheckRequest,
    ): Response<FcmTokenCheckResponse>
}
