package dev.comon.toss_watch.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class TokenRefreshRequest(
    @SerialName("refresh") val refresh: String,
)

@Serializable
data class TokenRefreshResponse(
    @SerialName("access") val access: String,
)

/**
 * Access Token 갱신 전용 API (`POST /api/v1/toss-watch/auth/refresh/`, 인증 불필요).
 *
 * [TokenAuthenticator]가 OkHttp 스레드에서 동기 실행하므로 suspend 대신
 * [Call]을 반환하며, 반드시 Authenticator가 없는 별도 클라이언트로 호출해야 한다.
 */
interface TokenRefreshApi {

    @POST("v1/toss-watch/auth/refresh/")
    fun refresh(@Body body: TokenRefreshRequest): Call<TokenRefreshResponse>
}
