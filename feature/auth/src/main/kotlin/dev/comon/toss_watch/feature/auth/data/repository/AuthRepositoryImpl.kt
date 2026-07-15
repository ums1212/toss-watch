package dev.comon.toss_watch.feature.auth.data.repository

import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.auth.data.remote.AuthApi
import dev.comon.toss_watch.feature.auth.data.remote.dto.GoogleLoginRequest
import dev.comon.toss_watch.feature.auth.data.remote.dto.toUserSession
import dev.comon.toss_watch.feature.auth.domain.model.UserSession
import dev.comon.toss_watch.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): NetworkResult<UserSession> =
        safeApiCall { authApi.loginWithGoogle(GoogleLoginRequest(idToken = idToken)) }
            .onSuccess { response ->
                // 세션 토큰은 Domain으로 흘려보내지 않고 여기서 즉시 암호화 저장한다.
                tokenStore.saveTokens(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                )
                // 콜드 스타트 시에도 토스 키 온보딩 분기를 판단할 수 있도록 영속 저장.
                tokenStore.setTossKeyRegistered(response.hasTossKey)
            }
            .map { it.toUserSession() }
}
