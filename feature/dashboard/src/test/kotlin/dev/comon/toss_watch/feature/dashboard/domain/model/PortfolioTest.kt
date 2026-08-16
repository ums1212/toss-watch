package dev.comon.toss_watch.feature.dashboard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [Portfolio]의 환율 환산 파생값(총 평가자산/평가손익/매수금액)을 검증한다.
 * 대시보드 요약 카드가 KRW+USD 합산 총액을 보여줄 때 이 값들을 사용한다.
 */
class PortfolioTest {

    @Test
    fun `환율이 유효하면 KRW와 USD 합계를 환산해 더한다`() {
        val portfolio = Portfolio(
            summary = PortfolioSummary(
                totalInvestmentKrw = 650_000.0,
                totalInvestmentUsd = 3_500.0,
                totalEvaluationKrw = 725_000.0,
                totalEvaluationUsd = 3_650.4,
                totalProfitLossKrw = 75_000.0,
                totalProfitLossUsd = 150.4,
                totalReturnRate = 26.92,
            ),
            securities = emptyList(),
            exchangeRate = 1_380.5,
        )

        assertEquals(725_000.0 + 3_650.4 * 1_380.5, portfolio.totalEvaluationInKrw, DELTA)
        assertEquals(75_000.0 + 150.4 * 1_380.5, portfolio.totalProfitLossInKrw, DELTA)
        assertEquals(650_000.0 + 3_500.0 * 1_380.5, portfolio.totalInvestmentInKrw, DELTA)
    }

    @Test
    fun `환율이 0이면 USD 합계를 반영하지 않고 KRW 합계만 반환한다`() {
        val portfolio = Portfolio(
            summary = PortfolioSummary(
                totalInvestmentKrw = 650_000.0,
                totalInvestmentUsd = 3_500.0,
                totalEvaluationKrw = 725_000.0,
                totalEvaluationUsd = 3_650.4,
                totalProfitLossKrw = 75_000.0,
                totalProfitLossUsd = 150.4,
                totalReturnRate = 26.92,
            ),
            securities = emptyList(),
            exchangeRate = 0.0,
        )

        assertEquals(725_000.0, portfolio.totalEvaluationInKrw, DELTA)
        assertEquals(75_000.0, portfolio.totalProfitLossInKrw, DELTA)
        assertEquals(650_000.0, portfolio.totalInvestmentInKrw, DELTA)
    }

    @Test
    fun `USD 보유가 없으면 환율과 무관하게 KRW 합계와 같다`() {
        val portfolio = Portfolio(
            summary = PortfolioSummary(
                totalInvestmentKrw = 800_000.0,
                totalInvestmentUsd = 0.0,
                totalEvaluationKrw = 725_000.0,
                totalEvaluationUsd = 0.0,
                totalProfitLossKrw = -75_000.0,
                totalProfitLossUsd = 0.0,
                totalReturnRate = -9.38,
            ),
            securities = emptyList(),
            exchangeRate = 1_380.5,
        )

        assertEquals(725_000.0, portfolio.totalEvaluationInKrw, DELTA)
        assertEquals(-75_000.0, portfolio.totalProfitLossInKrw, DELTA)
        assertEquals(800_000.0, portfolio.totalInvestmentInKrw, DELTA)
    }

    companion object {
        private const val DELTA = 0.001
    }
}
