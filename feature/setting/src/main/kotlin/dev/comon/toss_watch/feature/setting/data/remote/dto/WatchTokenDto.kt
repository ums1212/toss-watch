package dev.comon.toss_watch.feature.setting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("model_name") val modelName: String,
)

/**
 * PUT /users/fcm-token/ 응답 — message와 함께, 서버가 실제로 저장한 연동 정보
 * (model_name/uuid/linked_at)를 그대로 반환한다. 새 필드는 옛 서버 응답 형태와의
 * 호환을 위해 전부 기본값을 둔다.
 */
@Serializable
data class WatchTokenResponse(
    @SerialName("message") val message: String = "",
    @SerialName("has_fcm_token") val hasFcmToken: Boolean = false,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("uuid") val uuid: String? = null,
    @SerialName("linked_at") val linkedAt: String? = null,
)

/** GET /users/fcm-token/ 응답 — 등록 상태와 함께 model_name/uuid/linked_at을 반환한다(미등록 시 전부 null). */
@Serializable
data class WatchTokenStatusResponse(
    @SerialName("has_fcm_token") val hasFcmToken: Boolean,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("uuid") val uuid: String? = null,
    @SerialName("linked_at") val linkedAt: String? = null,
)
