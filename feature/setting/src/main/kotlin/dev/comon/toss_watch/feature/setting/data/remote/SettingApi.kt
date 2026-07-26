package dev.comon.toss_watch.feature.setting.data.remote

import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenResponse
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SettingApi {

    /** Wear OS 컴패니언 FCM 토큰 등록/갱신 (계정당 1개 — 멱등 PUT). */
    @PUT("v1/toss-watch/users/fcm-token/")
    suspend fun registerWatchToken(
        @Body body: WatchTokenRequest,
    ): Response<WatchTokenResponse>

    /** 워치 FCM 토큰 등록 상태 조회 — 등록된 경우 model_name/uuid를 함께 반환한다. */
    @GET("v1/toss-watch/users/fcm-token/")
    suspend fun getWatchTokenStatus(): Response<WatchTokenStatusResponse>
}
