package dev.comon.toss_watch.feature.auth.data.remote.dto

import dev.comon.toss_watch.feature.auth.domain.model.UserSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `POST /api/v1/auth/google/` 200 응답 바디. */
@Serializable
data class GoogleLoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("is_new_user") val isNewUser: Boolean = false,
    @SerialName("email") val email: String,
    @SerialName("has_toss_key") val hasTossKey: Boolean = false,
)

fun GoogleLoginResponse.toUserSession(): UserSession =
    UserSession(
        email = email,
        isNewUser = isNewUser,
        hasTossKey = hasTossKey,
    )
