package dev.comon.toss_watch.feature.setting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("model_name") val modelName: String,
)

@Serializable
data class WatchTokenResponse(
    @SerialName("message") val message: String = "",
)

/** GET /users/fcm-token/ 응답 — 등록 상태와 함께 model_name/uuid를 반환한다(미등록 시 둘 다 null). */
@Serializable
data class WatchTokenStatusResponse(
    @SerialName("has_fcm_token") val hasFcmToken: Boolean,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("uuid") val uuid: String? = null,
)
