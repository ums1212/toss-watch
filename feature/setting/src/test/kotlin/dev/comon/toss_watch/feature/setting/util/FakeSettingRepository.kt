package dev.comon.toss_watch.feature.setting.util

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingRepository : SettingRepository {

    var alarmsResult: NetworkResult<List<AlarmProfile>> = NetworkResult.Success(DEFAULT_ALARMS)
    var addResult: NetworkResult<AlarmProfile> = NetworkResult.Success(ADDED_ALARM)
    var toggleResult: NetworkResult<AlarmProfile>? = null
    var tokenResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    val portfolioStocks: MutableStateFlow<List<CachedStock>> = MutableStateFlow(DEFAULT_STOCKS)

    /** true면 [release] 호출 전까지 쓰기 요청을 지연시켜 isSaving 검증을 가능하게 한다. */
    var suspendUntilReleased: Boolean = false

    var lastAddedStockCode: String? = null
        private set
    var lastAddedHour: Int? = null
        private set
    var lastAddedMinute: Int? = null
        private set
    var lastToggledId: Long? = null
        private set
    var lastToggledEnabled: Boolean? = null
        private set
    var lastRegisteredToken: String? = null
        private set
    var registerInvocationCount: Int = 0
        private set

    private var gate = CompletableDeferred<Unit>()

    override suspend fun fetchAlarmProfiles(): NetworkResult<List<AlarmProfile>> = alarmsResult

    override fun observePortfolioStocks(): Flow<List<CachedStock>> = portfolioStocks

    override suspend fun addAlarmProfile(
        stockCode: String,
        hour: Int,
        minute: Int,
    ): NetworkResult<AlarmProfile> {
        lastAddedStockCode = stockCode
        lastAddedHour = hour
        lastAddedMinute = minute
        if (suspendUntilReleased) gate.await()
        return addResult
    }

    override suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> {
        lastToggledId = alarmId
        lastToggledEnabled = isEnabled
        if (suspendUntilReleased) gate.await()
        return toggleResult
            ?: NetworkResult.Success(
                DEFAULT_ALARMS.first { it.id == alarmId }.copy(isEnabled = isEnabled),
            )
    }

    override suspend fun registerWatchToken(fcmToken: String): NetworkResult<Unit> {
        registerInvocationCount++
        lastRegisteredToken = fcmToken
        if (suspendUntilReleased) gate.await()
        return tokenResult
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
                isEnabled = true,
            ),
            AlarmProfile(
                id = 2L,
                stockCode = "035420",
                stockName = "NAVER",
                hour = 15,
                minute = 30,
                isEnabled = false,
            ),
        )
        val ADDED_ALARM = AlarmProfile(
            id = 3L,
            stockCode = "000660",
            stockName = "SK하이닉스",
            hour = 10,
            minute = 15,
            isEnabled = true,
        )
        val DEFAULT_STOCKS = listOf(
            CachedStock(stockCode = "005930", stockName = "삼성전자"),
            CachedStock(stockCode = "035420", stockName = "NAVER"),
        )
    }
}
