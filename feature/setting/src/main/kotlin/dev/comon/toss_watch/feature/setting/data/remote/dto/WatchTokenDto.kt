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
