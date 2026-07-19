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
                // 토스 키 등록 여부를 세션 토큰보다 먼저 저장한다 — :app의 MainViewModel은
                // observeHasSession()과 observeTossKeyRegistered()를 combine해 라우팅하므로,
                // saveTokens()로 세션 플래그가 켜지는 시점에 토스 키 값이 이미 최신이어야
                // (hasSession=true, hasTossKey=false)라는 중간 상태를 거치지 않는다.
                tokenStore.setTossKeyRegistered(response.hasTossKey)
                // 세션 토큰은 Domain으로 흘려보내지 않고 여기서 즉시 암호화 저장한다.
                tokenStore.saveTokens(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                )
            }
            .map { it.toUserSession() }
}
