package dev.comon.toss_watch.feature.dashboard.util

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.CompletableDeferred

class FakeDashboardRepository : DashboardRepository {

    var accountsResult: NetworkResult<List<Account>> = NetworkResult.Success(DEFAULT_ACCOUNTS)
    var portfolioResult: NetworkResult<Portfolio> = NetworkResult.Success(DEFAULT_PORTFOLIO)

    /** accountSeq별로 다른 포트폴리오를 응답하고 싶을 때 사용 (지정 없으면 [portfolioResult] 사용). */
    var portfolioResultByAccountSeq: Map<Long, NetworkResult<Portfolio>> = emptyMap()

    /** true면 [release] 호출 전까지 응답을 지연시켜 로딩 상태 검증을 가능하게 한다. */
    var suspendUntilReleased: Boolean = false
    var accountsInvocationCount: Int = 0
        private set
    var portfolioInvocationCount: Int = 0
        private set

    private var gate = CompletableDeferred<Unit>()

    override suspend fun fetchAccounts(): NetworkResult<List<Account>> {
        accountsInvocationCount++
        if (suspendUntilReleased) gate.await()
        return accountsResult
    }

    override suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio> {
        portfolioInvocationCount++
        if (suspendUntilReleased) gate.await()
        return portfolioResultByAccountSeq[accountSeq] ?: portfolioResult
    }

    fun release() {
        gate.complete(Unit)
        gate = CompletableDeferred()
    }

    companion object {
        val DEFAULT_ACCOUNTS = listOf(
            Account(accountNo = "100012345678", accountSeq = 987654, accountType = "BROKERAGE"),
            Account(accountNo = "100098765432", accountSeq = 123456, accountType = "BROKERAGE"),
        )
        val DEFAULT_PORTFOLIO = Portfolio(
            summary = PortfolioSummary(
                totalInvestmentKrw = 650_000.0,
                totalInvestmentUsd = 0.0,
                totalEvaluationKrw = 725_000.0,
                totalEvaluationUsd = 0.0,
                totalProfitLossKrw = 75_000.0,
                totalProfitLossUsd = 0.0,
                totalReturnRate = 11.54,
            ),
            securities = listOf(
                HoldingStock(
                    stockCode = "005930",
                    stockName = "삼성전자",
                    currency = Currency.KRW,
                    quantity = 10.0,
                    averageBuyPrice = 65_000.0,
                    totalBuyAmount = 650_000.0,
                    currentPrice = 72_500.0,
                    totalEvaluationAmount = 725_000.0,
                    profitLoss = 75_000.0,
                    returnRate = 11.54,
                ),
            ),
        )
    }
}
