package dev.comon.toss_watch.core.network

import dev.comon.toss_watch.core.datastore.TokenStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401 Unauthorized 수신 시 Refresh Token으로 Access Token을 자가 갱신하고
 * 실패했던 요청을 새 토큰으로 자동 재시도하는 [Authenticator].
 *
 * 동작 규칙:
 * 1. 재시도 횟수가 [MAX_AUTH_RETRY]를 넘으면 포기한다 (무한 401 루프 방지).
 * 2. Refresh Token이 없으면 갱신 불가 → null 반환 (요청 실패 확정).
 * 3. 다른 스레드가 이미 토큰을 갱신해 뒀다면 그 토큰으로 즉시 재시도한다.
 * 4. 그 외에는 `POST /api/v1/toss-watch/auth/refresh/`를 동기 호출해 새 토큰을 발급받는다.
 * 5. Refresh Token 자체가 만료/위조(401)된 경우에만 세션을 정리한다.
 *    일시적 네트워크 오류로는 세션을 지우지 않는다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val tokenRefreshApi: dagger.Lazy<TokenRefreshApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.retryCount() >= MAX_AUTH_RETRY) {
            return null
        }

        val refreshToken = tokenStore.getRefreshToken() ?: return null

        synchronized(this) {
            val failedAccessToken = response.request
                .header(AuthInterceptor.HEADER_AUTHORIZATION)
                ?.removePrefix(AuthInterceptor.TOKEN_PREFIX)

            // 락 대기 중 다른 요청이 이미 갱신을 끝낸 경우: 새 토큰으로 바로 재시도.
            val currentAccessToken = tokenStore.getAccessToken()
            if (currentAccessToken != null && currentAccessToken != failedAccessToken) {
                return response.request.withBearerToken(currentAccessToken)
            }

            return when (val result = refreshAccessToken(refreshToken)) {
                is RefreshResult.Success -> {
                    tokenStore.updateAccessToken(result.accessToken)
                    response.request.withBearerToken(result.accessToken)
                }

                RefreshResult.SessionExpired -> {
                    // Refresh Token까지 만료/위조 확정: 세션 정리 → 재로그인 플로우로 유도.
                    tokenStore.clear()
                    null
                }

                RefreshResult.TransientFailure -> null
            }
        }
    }

    /**
     * `POST /api/v1/toss-watch/auth/refresh/`를 동기 호출한다.
     * Authenticator는 OkHttp 스레드에서 실행되므로 [retrofit2.Call.execute]를 사용하며,
     * 재귀 방지를 위해 Authenticator가 없는 @RefreshClient 클라이언트를 사용한다.
     */
    private fun refreshAccessToken(refreshToken: String): RefreshResult =
        try {
            val response = tokenRefreshApi.get()
                .refresh(TokenRefreshRequest(refresh = refreshToken))
                .execute()
            val body = response.body()
            when {
                response.isSuccessful && body != null -> RefreshResult.Success(body.access)
                response.code() == 401 -> RefreshResult.SessionExpired
                else -> RefreshResult.TransientFailure
            }
        } catch (e: IOException) {
            RefreshResult.TransientFailure
        }

    private sealed interface RefreshResult {
        data class Success(val accessToken: String) : RefreshResult
        data object SessionExpired : RefreshResult
        data object TransientFailure : RefreshResult
    }

    private fun Request.withBearerToken(token: String): Request =
        newBuilder()
            .header(AuthInterceptor.HEADER_AUTHORIZATION, "${AuthInterceptor.TOKEN_PREFIX}$token")
            .build()

    private fun Response.retryCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_AUTH_RETRY = 2
    }
}
