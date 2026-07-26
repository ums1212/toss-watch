package dev.comon.toss_watch.feature.alarm.data.repository

import dev.comon.toss_watch.core.database.PortfolioStockCache
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.core.network.safeApiCallNoContent
import dev.comon.toss_watch.feature.alarm.data.remote.AlarmApi
import dev.comon.toss_watch.feature.alarm.data.remote.dto.AlarmProfileRequest
import dev.comon.toss_watch.feature.alarm.data.remote.dto.AlarmToggleRequest
import dev.comon.toss_watch.feature.alarm.data.remote.dto.formatAlarmTime
import dev.comon.toss_watch.feature.alarm.data.remote.dto.toAlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmApi: AlarmApi,
    private val portfolioStockCache: PortfolioStockCache,
) : AlarmRepository {

    /**
     * 알림 프로필의 단일 소스 — AlarmScreen(종목별 요약)과 AlarmDetailScreen(종목별 상세)이
     * 이 캐시를 공유 구독하므로, 한쪽에서의 변경이 다른 화면에도 재조회 없이 즉시 반영된다.
     */
    private val alarmProfiles = MutableStateFlow<List<AlarmProfile>>(emptyList())

    override fun observeAlarmProfiles(): Flow<List<AlarmProfile>> = alarmProfiles.asStateFlow()

    override suspend fun refreshAlarmProfiles(): NetworkResult<Unit> =
        safeApiCall { alarmApi.getAlarmProfiles() }
            .map { list -> list.map { it.toAlarmProfile() } }
            .onSuccess { alarmProfiles.value = it }
            .map { }

    override fun observePortfolioStocks(): Flow<List<CachedStock>> =
        portfolioStockCache.observeStocks()

    override suspend fun addAlarmProfile(
        stockCode: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile> =
        safeApiCall {
            alarmApi.createAlarmProfile(
                AlarmProfileRequest(
                    stockCode = stockCode,
                    alarmTime = formatAlarmTime(hour, minute),
                    daysOfWeek = daysOfWeek,
                ),
            )
        }
            .map { it.toAlarmProfile() }
            .onSuccess { profile -> alarmProfiles.update { it + profile } }

    override suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> =
        safeApiCall {
            alarmApi.updateAlarmProfile(
                alarmId = alarmId,
                body = AlarmToggleRequest(isEnabled = isEnabled),
            )
        }
            .map { it.toAlarmProfile() }
            .onSuccess { profile ->
                alarmProfiles.update { list -> list.map { if (it.id == profile.id) profile else it } }
            }

    override suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit> =
        safeApiCallNoContent { alarmApi.deleteAlarmProfile(alarmId) }
            .onSuccess { alarmProfiles.update { list -> list.filterNot { it.id == alarmId } } }

    override fun setAlarmEnabledInCache(alarmId: Long, isEnabled: Boolean) {
        alarmProfiles.update { list ->
            list.map { if (it.id == alarmId) it.copy(isEnabled = isEnabled) else it }
        }
    }
}
