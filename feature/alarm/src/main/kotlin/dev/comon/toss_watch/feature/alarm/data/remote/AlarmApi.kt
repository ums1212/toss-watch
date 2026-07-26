package dev.comon.toss_watch.feature.alarm.data.remote

import dev.comon.toss_watch.feature.alarm.data.remote.dto.AlarmProfileRequest
import dev.comon.toss_watch.feature.alarm.data.remote.dto.AlarmProfileResponse
import dev.comon.toss_watch.feature.alarm.data.remote.dto.AlarmToggleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AlarmApi {

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

    /** 알림 프로필 삭제 — 성공 시 204 No Content. */
    @DELETE("v1/toss-watch/notifications/{id}/")
    suspend fun deleteAlarmProfile(
        @Path("id") alarmId: Long,
    ): Response<Unit>
}
