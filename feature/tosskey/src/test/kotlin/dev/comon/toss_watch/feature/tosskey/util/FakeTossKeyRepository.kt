package dev.comon.toss_watch.feature.tosskey.util

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * 결과를 주입할 수 있는 TossKeyRepository 페이크.
 * [suspendUntilReleased]가 true면 [release] 호출 전까지 응답을 지연시켜
 * isSaving(in-flight) 상태를 결정적으로 검증할 수 있다.
 */
class FakeTossKeyRepository : TossKeyRepository {

    var result: NetworkResult<Unit> = NetworkResult.Success(Unit)
    var suspendUntilReleased: Boolean = false

    var invocationCount: Int = 0
        private set
    var lastClientId: String? = null
        private set
    var lastClientSecret: String? = null
        private set

    private var gate = CompletableDeferred<Unit>()

    override suspend fun registerTossKey(
        clientId: String,
        clientSecret: String,
    ): NetworkResult<Unit> {
        invocationCount++
        lastClientId = clientId
        lastClientSecret = clientSecret
        if (suspendUntilReleased) gate.await()
        return result
    }

    fun release() {
        gate.complete(Unit)
        gate = CompletableDeferred()
    }
}
