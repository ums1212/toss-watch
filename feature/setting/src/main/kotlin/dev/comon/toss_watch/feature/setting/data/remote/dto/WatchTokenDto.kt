package dev.comon.toss_watch.feature.setting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("device_type") val deviceType: String = DEVICE_TYPE_WEAR_OS,
) {
    companion object {
        const val DEVICE_TYPE_WEAR_OS = "WEAR_OS"
    }
}

@Serializable
data class WatchTokenResponse(
    @SerialName("fcm_token") val fcmToken: String,
)
