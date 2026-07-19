package dev.comon.toss_watch.feature.tosskey.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `POST /api/v1/toss-watch/users/toss-key/` 요청 바디. */
@Serializable
data class TossKeyRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)
