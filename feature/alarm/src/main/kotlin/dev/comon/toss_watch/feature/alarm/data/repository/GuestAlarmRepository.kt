package dev.comon.toss_watch.feature.alarm.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.data.guest.GuestAlarmData
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 게스트(더미 데이터 체험) 모드용 [AlarmRepository] — [AlarmRepositoryImpl]과 동일하게
 * 인메모리 [MutableStateFlow] 캐시를 단일 소스로 두고 add/toggle/delete를 실제로 반영한다
 * (CLAUDE.md의 "공유 캐시를 구독하는 화면은 재조회 없이 즉시 반영된다" 계약을 게스트에서도 유지).
 *
 * [dev.comon.toss_watch.feature.alarm.presentation.alarm.AlarmViewModel]과
 * [dev.comon.toss_watch.feature.alarm.presentation.alarmdetail.AlarmDetailViewModel]이 둘 다 `init`에서 `refreshAlarmProfiles()`를
 * 호출하므로, 매번 시드로 덮어쓰면 상세 화면에 재진입할 때 게스트가 방금 추가한 알림이 사라진다.
 * 그래서 [GuestModeStore.guestSessionId](게스트 재진입마다 증가)를 마지막으로 시딩한 세션과 비교해,
 * 같은 세션 안에서는 사용자의 변경을 보존하고 게스트를 재진입했을 때만 초기 시드로 되돌린다.
 */
@Singleton
internal class GuestAlarmRepository @Inject constructor(
    private val guestModeStore: GuestModeStore,
) : AlarmRepository {

    private val alarmProfiles = MutableStateFlow<List<AlarmProfile>>(emptyList())
    private val portfolioStocks = MutableStateFlow(GuestAlarmData.PORTFOLIO_STOCKS)

    /** 마지막으로 시드를 채운 게스트 세션 ID. 아직 한 번도 시딩하지 않았으면 -1. */
    @Volatile
    private var seededSessionId: Long = NOT_SEEDED

    private val nextAlarmId = AtomicLong(GuestAlarmData.SEED_ID_UPPER_BOUND)

    override fun observeAlarmProfiles(): Flow<List<AlarmProfile>> = alarmProfiles.asStateFlow()

    override suspend fun refreshAlarmProfiles(): NetworkResult<Unit> {
        val currentSessionId = guestModeStore.guestSessionId
        if (seededSessionId != currentSessionId) {
            alarmProfiles.value = GuestAlarmData.SEED_ALARMS
            seededSessionId = currentSessionId
        }
        return NetworkResult.Success(Unit)
    }

    override fun observePortfolioStocks(): Flow<List<CachedStock>> = portfolioStocks.asStateFlow()

    override suspend fun addAlarmProfile(
        stockCode: String,
        stockName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile> {
        val profile = AlarmProfile(
            id = nextAlarmId.incrementAndGet(),
            stockCode = stockCode,
            stockName = stockName,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
            isEnabled = true,
        )
        alarmProfiles.update { it + profile }
        return NetworkResult.Success(profile)
    }

    override suspend fun updateAlarmProfile(alarmId: Long, isEnabled: Boolean): NetworkResult<AlarmProfile> {
        setAlarmEnabledInCache(alarmId, isEnabled)
        val updated = alarmProfiles.value.firstOrNull { it.id == alarmId }
            ?: return NetworkResult.ApiError(code = 404, message = "존재하지 않는 알림이에요.")
        return NetworkResult.Success(updated)
    }

    override suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit> {
        alarmProfiles.update { list -> list.filterNot { it.id == alarmId } }
        return NetworkResult.Success(Unit)
    }

    override fun setAlarmEnabledInCache(alarmId: Long, isEnabled: Boolean) {
        alarmProfiles.update { list ->
            list.map { if (it.id == alarmId) it.copy(isEnabled = isEnabled) else it }
        }
    }

    private companion object {
        const val NOT_SEEDED = -1L
    }
}
