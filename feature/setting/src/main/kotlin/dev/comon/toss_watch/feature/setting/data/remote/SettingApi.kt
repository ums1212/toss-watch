package dev.comon.toss_watch.feature.setting.data.remote

import dev.comon.toss_watch.feature.setting.data.remote.dto.AlarmProfileRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.AlarmProfileResponse
import dev.comon.toss_watch.feature.setting.data.remote.dto.AlarmToggleRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SettingApi {

    @GET("v1/toss-watch/notifications/")
    suspend fun getAlarmProfiles(): Response<List<AlarmProfileResponse>>

    /** 새 알림 프로필 등록 (stock_code, alarm_time). */
    @POST("v1/toss-watch/notifications/")
    suspend fun createAlarmProfile(
        @Body body: AlarmProfileRequest,
    ): Response<AlarmProfileResponse>

    /** 알림 프로필 부분 갱신 (활성 토글). */
    @PATCH("v1/toss-watch/notifications/{id}/")
    suspend fun updateAlarmProfile(
        @Path("id") alarmId: Long,
        @Body body: AlarmToggleRequest,
    ): Response<AlarmProfileResponse>

    /** Wear OS 컴패니언 FCM 토큰 등록/갱신 (계정당 1개 — 멱등 PUT). */
    @PUT("v1/toss-watch/users/fcm-token/")
    suspend fun registerWatchToken(
        @Body body: WatchTokenRequest,
    ): Response<WatchTokenResponse>
}
