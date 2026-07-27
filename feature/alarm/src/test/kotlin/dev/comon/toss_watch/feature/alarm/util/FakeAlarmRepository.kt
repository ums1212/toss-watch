package dev.comon.toss_watch.feature.alarm.util

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * [dev.comon.toss_watch.feature.alarm.data.repository.AlarmRepositoryImpl]과 동일하게
 * 인메모리 캐시([alarmProfiles])를 단일 소스로 두고, 성공한 쓰기 요청마다 캐시를 갱신한다 —
 * 실제 구현과 동일한 "캐시를 구독하는 화면은 재조회 없이 즉시 반영된다" 계약을 테스트에서도 검증하기 위함.
 */
class FakeAlarmRepository : AlarmRepository {

    val alarmProfiles: MutableStateFlow<List<AlarmProfile>> = MutableStateFlow(emptyList())

    /** [refreshAlarmProfiles] 성공 시 캐시에 채워 넣을 목록. */
    var seedAlarms: List<AlarmProfile> = DEFAULT_ALARMS
    var refreshResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    var addResult: NetworkResult<AlarmProfile> = NetworkResult.Success(ADDED_ALARM)
    var toggleResult: NetworkResult<AlarmProfile>? = null
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    val portfolioStocks: MutableStateFlow<List<CachedStock>> = MutableStateFlow(DEFAULT_STOCKS)

    /** true면 [release] 호출 전까지 쓰기 요청을 지연시켜 isSaving/디바운스 검증을 가능하게 한다. */
    var suspendUntilReleased: Boolean = false
    private var gate = CompletableDeferred<Unit>()

    var refreshInvocationCount: Int = 0
        private set
    var lastAddedStockCode: String? = null
        private set
    var lastAddedStockName: String? = null
        private set
    var lastAddedHour: Int? = null
        private set
    var lastAddedMinute: Int? = null
        private set
    var lastAddedDaysOfWeek: List<Int>? = null
        private set
    var toggleInvocationCount: Int = 0
        private set
    var lastToggledId: Long? = null
        private set
    var lastToggledEnabled: Boolean? = null
        private set
    var deleteInvocationCount: Int = 0
        private set
    var lastDeletedId: Long? = null
        private set

    override fun observeAlarmProfiles(): Flow<List<AlarmProfile>> = alarmProfiles

    override suspend fun refreshAlarmProfiles(): NetworkResult<Unit> {
        refreshInvocationCount++
        if (suspendUntilReleased) gate.await()
        if (refreshResult is NetworkResult.Success) {
            alarmProfiles.value = seedAlarms
        }
        return refreshResult
    }

    override fun observePortfolioStocks(): Flow<List<CachedStock>> = portfolioStocks

    override suspend fun addAlarmProfile(
        stockCode: String,
        stockName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile> {
        lastAddedStockCode = stockCode
        lastAddedStockName = stockName
        lastAddedHour = hour
        lastAddedMinute = minute
        lastAddedDaysOfWeek = daysOfWeek
        if (suspendUntilReleased) gate.await()

        val result = addResult
        if (result is NetworkResult.Success) {
            alarmProfiles.update { it + result.data }
        }
        return result
    }

    override suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> {
        toggleInvocationCount++
        lastToggledId = alarmId
        lastToggledEnabled = isEnabled
        if (suspendUntilReleased) gate.await()

        val result = toggleResult
            ?: NetworkResult.Success(
                alarmProfiles.value.first { it.id == alarmId }.copy(isEnabled = isEnabled),
            )
        if (result is NetworkResult.Success) {
            alarmProfiles.update { list -> list.map { if (it.id == result.data.id) result.data else it } }
        }
        return result
    }

    override suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit> {
        deleteInvocationCount++
        lastDeletedId = alarmId
        if (suspendUntilReleased) gate.await()

        val result = deleteResult
        if (result is NetworkResult.Success) {
            alarmProfiles.update { list -> list.filterNot { it.id == alarmId } }
        }
        return result
    }

    override fun setAlarmEnabledInCache(alarmId: Long, isEnabled: Boolean) {
        alarmProfiles.update { list ->
            list.map { if (it.id == alarmId) it.copy(isEnabled = isEnabled) else it }
        }
    }

    fun release() {
        gate.complete(Unit)
        gate = CompletableDeferred()
    }

    companion object {
        val DEFAULT_ALARMS = listOf(
            AlarmProfile(
                id = 1L,
                stockCode = "005930",
                stockName = "삼성전자",
                hour = 9,
                minute = 0,
                daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6),
                isEnabled = true,
            ),
            AlarmProfile(
                id = 2L,
                stockCode = "035420",
                stockName = "NAVER",
                hour = 15,
                minute = 30,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
                isEnabled = false,
            ),
        )
        val ADDED_ALARM = AlarmProfile(
            id = 3L,
            stockCode = "000660",
            stockName = "SK하이닉스",
            hour = 10,
            minute = 15,
            daysOfWeek = listOf(0, 1, 2, 3, 4, 5),
            isEnabled = true,
        )
        val DEFAULT_STOCKS = listOf(
            CachedStock(stockCode = "005930", stockName = "삼성전자"),
            CachedStock(stockCode = "035420", stockName = "NAVER"),
        )
    }
}
