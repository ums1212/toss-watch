package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.core.database.PortfolioStockCache
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.dashboard.data.remote.DashboardApi
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.PortfolioResponse
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toAccount
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toPortfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val portfolioStockCache: PortfolioStockCache,
) : DashboardRepository {

    override suspend fun fetchAccounts(): NetworkResult<List<Account>> =
        safeApiCall { dashboardApi.getAccounts() }
            .map { list -> list.map { it.toAccount() } }

    /**
     * 포트폴리오 조회에 성공할 때마다 보유 종목을 [PortfolioStockCache]에 통째로 교체 기록한다.
     * `feature:setting`이 이 캐시를 구독해 알림 추가 종목 후보로 쓰므로,
     * 계좌를 바꿔 재조회할 때도 캐시가 "지금 화면에 보이는 종목" 기준으로 항상 갱신된다.
     */
    override suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio> =
        safeApiCall { dashboardApi.getPortfolio(accountSeq) }
            .onSuccess { response -> portfolioStockCache.replaceStocks(response.toCachedStocks()) }
            .map { it.toPortfolio() }

    private fun PortfolioResponse.toCachedStocks(): List<CachedStock> =
        securities.map { CachedStock(stockCode = it.stockCode, stockName = it.stockName) }
}
