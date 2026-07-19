package dev.comon.watch_app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 2-5. 워치 FCM 토큰 등록 여부 확인 (`POST /fcm-token/check/`) 요청 바디. */
@Serializable
data class FcmTokenCheckRequest(
    @SerialName("fcm_token") val fcmToken: String,
)

/** 2-5. 워치 FCM 토큰 등록 여부 확인 응답 바디. */
@Serializable
data class FcmTokenCheckResponse(
    @SerialName("registered") val registered: Boolean,
)
