package dev.comon.toss_watch.feature.setting.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.setting.data.remote.SettingApi
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenRequest
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingRepositoryImpl @Inject constructor(
    private val settingApi: SettingApi,
    private val tokenStore: TokenStore,
    private val guestModeStore: GuestModeStore,
) : SettingRepository {

    override suspend fun registerWatchToken(
        fcmToken: String,
        uuid: String,
        modelName: String,
    ): NetworkResult<Unit> =
        safeApiCall {
            settingApi.registerWatchToken(
                WatchTokenRequest(fcmToken = fcmToken, uuid = uuid, modelName = modelName),
            )
        }
            .onSuccess { tokenStore.setPairedWatch(modelName = modelName, uuid = uuid) }
            .map { }

    override fun observePairedWatch(): Flow<PairedWatchInfo?> = tokenStore.observePairedWatch()

    override suspend fun syncPairedWatch(): NetworkResult<Unit> =
        safeApiCall { settingApi.getWatchTokenStatus() }
            .onSuccess { status ->
                if (status.hasFcmToken && !status.uuid.isNullOrBlank()) {
                    tokenStore.setPairedWatch(modelName = status.modelName, uuid = status.uuid)
                } else {
                    tokenStore.clearPairedWatch()
                }
            }
            .map { }

    override fun logout() {
        tokenStore.clear()
        // 게스트로 진입했을 수도 있으니 잔여 플래그를 함께 정리한다 — 실 로그아웃이었다면 이미
        // false 상태라 no-op이다.
        guestModeStore.exitGuestMode()
    }

    override fun observeGuestMode() = guestModeStore.observeGuestMode()
}
