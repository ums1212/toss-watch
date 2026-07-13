package dev.comon.toss_watch.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequest(
    @SerialName("id_token") val idToken: String,
)
