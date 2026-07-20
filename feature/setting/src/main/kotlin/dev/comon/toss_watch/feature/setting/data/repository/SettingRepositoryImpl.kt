package dev.comon.toss_watch.feature.setting.data.repository

import dev.comon.toss_watch.core.database.PortfolioStockCache
import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.setting.data.remote.SettingApi
import dev.comon.toss_watch.feature.setting.data.remote.dto.AlarmProfileRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.AlarmToggleRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenRequest
import dev.comon.toss_watch.feature.setting.data.remote.dto.formatAlarmTime
import dev.comon.toss_watch.feature.setting.data.remote.dto.toAlarmProfile
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingRepositoryImpl @Inject constructor(
    private val settingApi: SettingApi,
    private val portfolioStockCache: PortfolioStockCache,
    private val tokenStore: TokenStore,
) : SettingRepository {

    override suspend fun fetchAlarmProfiles(): NetworkResult<List<AlarmProfile>> =
        safeApiCall { settingApi.getAlarmProfiles() }
            .map { list -> list.map { it.toAlarmProfile() } }

    override fun observePortfolioStocks(): Flow<List<CachedStock>> =
        portfolioStockCache.observeStocks()

    override suspend fun addAlarmProfile(
        stockCode: String,
        hour: Int,
        minute: Int,
    ): NetworkResult<AlarmProfile> =
        safeApiCall {
            settingApi.createAlarmProfile(
                AlarmProfileRequest(
                    stockCode = stockCode,
                    alarmTime = formatAlarmTime(hour, minute),
                ),
            )
        }.map { it.toAlarmProfile() }

    override suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> =
        safeApiCall {
            settingApi.updateAlarmProfile(
                alarmId = alarmId,
                body = AlarmToggleRequest(isEnabled = isEnabled),
            )
        }.map { it.toAlarmProfile() }

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

    override fun logout() = tokenStore.clear()
}
