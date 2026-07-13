package dev.comon.toss_watch.feature.auth.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.domain.model.UserSession

interface AuthRepository {

    /**
     * Google ID Token을 서버로 보내 검증받고 서비스 JWT 세션을 수립한다.
     * 성공 시 토큰 저장까지 완료된 상태의 [UserSession]을 반환한다.
     */
    suspend fun loginWithGoogle(idToken: String): NetworkResult<UserSession>
}
