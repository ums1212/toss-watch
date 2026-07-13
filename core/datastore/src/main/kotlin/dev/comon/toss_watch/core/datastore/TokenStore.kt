package dev.comon.toss_watch.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * 세션 토큰(Access/Refresh JWT)의 보안 저장소 계약.
 *
 * `:core:network`의 인터셉터/Authenticator가 이 인터페이스에만 의존하고,
 * 실제 암호화 구현([DataStoreTokenStore])은 이 모듈 내부에 캡슐화한다.
 */
interface TokenStore {

    /**
     * 세션(Refresh JWT) 존재 여부의 반응형 스트림.
     * :app 최상위 라우터가 구독해 Auth/Dashboard 루트를 동적으로 전환한다.
     * 토큰 평문은 흘리지 않고 존재 여부만 노출한다.
     */
    fun observeHasSession(): Flow<Boolean>

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun saveTokens(accessToken: String, refreshToken: String)

    fun updateAccessToken(accessToken: String)

    fun clear()
}
