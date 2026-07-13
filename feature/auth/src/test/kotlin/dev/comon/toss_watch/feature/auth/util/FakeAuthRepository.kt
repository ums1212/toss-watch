package dev.comon.toss_watch.feature.auth.util

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.domain.model.UserSession
import dev.comon.toss_watch.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * 결과를 주입할 수 있는 AuthRepository 페이크.
 * [suspendUntilReleased]가 true면 [release] 호출 전까지 응답을 지연시켜
 * 로딩 상태(in-flight)를 결정적으로 검증할 수 있다.
 */
class FakeAuthRepository : AuthRepository {

    var result: NetworkResult<UserSession> = NetworkResult.Success(DEFAULT_SESSION)
    var suspendUntilReleased: Boolean = false

    var invocationCount: Int = 0
        private set
    var lastIdToken: String? = null
        private set

    private val gate = CompletableDeferred<Unit>()

    override suspend fun loginWithGoogle(idToken: String): NetworkResult<UserSession> {
        invocationCount++
        lastIdToken = idToken
        if (suspendUntilReleased) gate.await()
        return result
    }

    fun release() {
        gate.complete(Unit)
    }

    companion object {
        val DEFAULT_SESSION = UserSession(
            email = "user@gmail.com",
            isNewUser = false,
            hasTossKey = false,
        )
    }
}
