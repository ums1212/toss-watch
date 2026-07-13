package dev.comon.toss_watch.feature.dashboard.util

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.CompletableDeferred

class FakeDashboardRepository : DashboardRepository {

    var assetsResult: NetworkResult<UserAssets> = NetworkResult.Success(DEFAULT_ASSETS)
    var tickersResult: NetworkResult<List<TargetTicker>> =
        NetworkResult.Success(DEFAULT_TICKERS)

    /** true면 [release] 호출 전까지 응답을 지연시켜 로딩 상태 검증을 가능하게 한다. */
    var suspendUntilReleased: Boolean = false
    var assetsInvocationCount: Int = 0
        private set
    var tickersInvocationCount: Int = 0
        private set

    private var gate = CompletableDeferred<Unit>()

    override suspend fun fetchUserAssets(): NetworkResult<UserAssets> {
        assetsInvocationCount++
        if (suspendUntilReleased) gate.await()
        return assetsResult
    }

    override suspend fun fetchTargetTickers(): NetworkResult<List<TargetTicker>> {
        tickersInvocationCount++
        if (suspendUntilReleased) gate.await()
        return tickersResult
    }

    fun release() {
        gate.complete(Unit)
        gate = CompletableDeferred()
    }

    companion object {
        val DEFAULT_ASSETS = UserAssets(
            totalAssets = 10_000_000L,
            totalProfit = 250_000L,
            profitRate = 2.56,
        )
        val DEFAULT_TICKERS = listOf(
            TargetTicker(code = "005930", name = "삼성전자", currentPrice = 78_400L, changeRate = 1.42),
            TargetTicker(code = "035420", name = "NAVER", currentPrice = 187_500L, changeRate = -0.83),
        )
    }
}
