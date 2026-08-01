package dev.comon.toss_watch.feature.dashboard.data.guest

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.data.repository.GuestDashboardRepository
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 차트/요약 위젯(PortfolioChartCard 등)이 의존하는 불변식 — "계좌 요약은 보유 종목 합계와
 * 일치한다" — 를 검증한다. 종목 데이터를 수정했을 때 요약을 손으로 맞추지 않아도 되도록
 * [GuestDashboardData]가 요약(PortfolioSummary)을 항상 계산해서 채우기 때문에 성립해야 하는 조건이다.
 */
class GuestDashboardDataTest {

    @Test
    fun `계좌별 요약이 보유 종목 합계와 일치한다`() {
        GuestDashboardData.PORTFOLIOS.values.forEach { portfolio ->
            val krw = portfolio.securities.filter { it.currency == Currency.KRW }
            val usd = portfolio.securities.filter { it.currency == Currency.USD }

            assertEquals(krw.sumOf { it.totalBuyAmount }, portfolio.summary.totalInvestmentKrw, DELTA)
            assertEquals(krw.sumOf { it.totalEvaluationAmount }, portfolio.summary.totalEvaluationKrw, DELTA)
            assertEquals(usd.sumOf { it.totalBuyAmount }, portfolio.summary.totalInvestmentUsd, DELTA)
            assertEquals(usd.sumOf { it.totalEvaluationAmount }, portfolio.summary.totalEvaluationUsd, DELTA)
            assertEquals(
                portfolio.summary.totalEvaluationKrw - portfolio.summary.totalInvestmentKrw,
                portfolio.summary.totalProfitLossKrw,
                DELTA,
            )
            assertEquals(
                portfolio.summary.totalEvaluationUsd - portfolio.summary.totalInvestmentUsd,
                portfolio.summary.totalProfitLossUsd,
                DELTA,
            )
        }
    }

    @Test
    fun `계좌마다 통화가 혼합되거나 손익이 혼합돼 차트가 의미 있게 보인다`() {
        val mainPortfolio = GuestDashboardData.PORTFOLIOS.getValue(GuestDashboardData.ACCOUNT_MAIN.accountSeq)

        assertTrue(mainPortfolio.securities.any { it.currency == Currency.KRW })
        assertTrue(mainPortfolio.securities.any { it.currency == Currency.USD })
        assertTrue(mainPortfolio.securities.any { it.profitLoss > 0 })
        assertTrue(mainPortfolio.securities.any { it.profitLoss < 0 })
    }

    @Test
    fun `알 수 없는 계좌 시퀀스는 메인 계좌 포트폴리오로 대체된다`() =
        runTest {
            val repository = GuestDashboardRepository()

            val result = repository.fetchPortfolio(accountSeq = 999_999L)

            assertTrue(result is NetworkResult.Success)
            assertEquals(
                GuestDashboardData.PORTFOLIOS.getValue(GuestDashboardData.ACCOUNT_MAIN.accountSeq),
                (result as NetworkResult.Success).data,
            )
        }

    companion object {
        private const val DELTA = 0.001
    }
}
