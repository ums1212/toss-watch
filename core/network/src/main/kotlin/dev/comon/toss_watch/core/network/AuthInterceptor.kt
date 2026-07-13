package dev.comon.toss_watch.core.network

import dev.comon.toss_watch.core.datastore.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Django 서버로 나가는 모든 요청에 `Authorization: Bearer <JWT>` 헤더를 부착하는 인터셉터.
 *
 * 이미 Authorization 헤더가 지정된 요청(예: 토큰 갱신 요청)이나
 * 저장된 Access Token이 없는 경우(비로그인 상태)는 그대로 통과시킨다.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        val accessToken = tokenStore.getAccessToken()
            ?: return chain.proceed(request)

        val authenticated = request.newBuilder()
            .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX$accessToken")
            .build()
        return chain.proceed(authenticated)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val TOKEN_PREFIX = "Bearer "
    }
}
