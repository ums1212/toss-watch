package dev.comon.toss_watch.feature.alarm.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * [remote]/[guest] 중 현재 세션이 게스트 모드인지에 따라 호출을 위임하는 라우터.
 *
 * `@Binds`는 컴파일 타임에 구현체가 고정되어 런타임 전환이 불가능하므로,
 * Hilt 모듈에서 이 라우터를 `@Provides`로 조립해 [AlarmRepository]로 주입한다.
 *
 * [observeAlarmProfiles]/[observePortfolioStocks]는 Flow를 노출하므로 suspend 호출처럼
 * 호출 시점에 한 번만 분기해서는 안 된다 — 게스트 진입/이탈이 구독 중인 화면에 즉시 반영되도록
 * [GuestModeStore.observeGuestMode]를 [flatMapLatest]로 갈아탄다.
 */
internal class AlarmRepositoryRouter(
    private val remote: AlarmRepository,
    private val guest: AlarmRepository,
    private val guestModeStore: GuestModeStore,
) : AlarmRepository {

    private fun current(): AlarmRepository =
        if (guestModeStore.isGuestMode()) guest else remote

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAlarmProfiles(): Flow<List<AlarmProfile>> =
        guestModeStore.observeGuestMode().flatMapLatest { isGuest ->
            if (isGuest) guest.observeAlarmProfiles() else remote.observeAlarmProfiles()
        }

    override suspend fun refreshAlarmProfiles(): NetworkResult<Unit> = current().refreshAlarmProfiles()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePortfolioStocks(): Flow<List<CachedStock>> =
        guestModeStore.observeGuestMode().flatMapLatest { isGuest ->
            if (isGuest) guest.observePortfolioStocks() else remote.observePortfolioStocks()
        }

    override suspend fun addAlarmProfile(
        stockCode: String,
        stockName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile> = current().addAlarmProfile(stockCode, stockName, hour, minute, daysOfWeek)

    override suspend fun updateAlarmProfile(alarmId: Long, isEnabled: Boolean): NetworkResult<AlarmProfile> =
        current().updateAlarmProfile(alarmId, isEnabled)

    override suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit> =
        current().deleteAlarmProfile(alarmId)

    override fun setAlarmEnabledInCache(alarmId: Long, isEnabled: Boolean) {
        current().setAlarmEnabledInCache(alarmId, isEnabled)
    }
}
