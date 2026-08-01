package dev.comon.toss_watch.feature.setting.util

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingRepository : SettingRepository {

    var tokenResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    val pairedWatch: MutableStateFlow<PairedWatchInfo?> = MutableStateFlow(null)
    val guestMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** [syncPairedWatch] 호출 결과. 성공 시 [syncedWatch] 값을 [pairedWatch]에 반영한다(서버 복원 시나리오 검증용). */
    var syncResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    /** null이면 미등록(clear), 값이 있으면 서버 복원 값으로 취급 — [registerWatchToken]을 거치지 않고 재설치 시나리오를 흉내낸다. */
    var syncedWatch: PairedWatchInfo? = null
    var syncInvocationCount: Int = 0
        private set

    /** true면 [release] 호출 전까지 쓰기 요청을 지연시켜 isSaving 검증을 가능하게 한다. */
    var suspendUntilReleased: Boolean = false

    var lastRegisteredToken: String? = null
        private set
    var lastRegisteredUuid: String? = null
        private set
    var lastRegisteredModelName: String? = null
        private set
    var registerInvocationCount: Int = 0
        private set

    private var gate = CompletableDeferred<Unit>()

    override suspend fun registerWatchToken(
        fcmToken: String,
        uuid: String,
        modelName: String,
    ): NetworkResult<Unit> {
        registerInvocationCount++
        lastRegisteredToken = fcmToken
        lastRegisteredUuid = uuid
        lastRegisteredModelName = modelName
        if (suspendUntilReleased) gate.await()
        if (tokenResult is NetworkResult.Success) {
            pairedWatch.value = PairedWatchInfo(modelName = modelName, uuid = uuid)
        }
        return tokenResult
    }

    override fun observePairedWatch(): Flow<PairedWatchInfo?> = pairedWatch

    override suspend fun syncPairedWatch(): NetworkResult<Unit> {
        syncInvocationCount++
        if (syncResult is NetworkResult.Success) {
            pairedWatch.value = syncedWatch
        }
        return syncResult
    }

    var logoutInvocationCount: Int = 0
        private set

    override fun logout() {
        logoutInvocationCount++
        guestMode.value = false
    }

    override fun observeGuestMode(): Flow<Boolean> = guestMode

    fun release() {
        gate.complete(Unit)
        gate = CompletableDeferred()
    }
}
