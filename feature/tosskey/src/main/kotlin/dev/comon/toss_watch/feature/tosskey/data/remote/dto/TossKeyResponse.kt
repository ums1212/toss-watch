package dev.comon.toss_watch.feature.tosskey.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `POST /api/v1/toss-watch/users/toss-key/` 200 응답 바디. */
@Serializable
data class TossKeyResponse(
    @SerialName("message") val message: String,
    @SerialName("toss_client_id") val tossClientId: String,
)
