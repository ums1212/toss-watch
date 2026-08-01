package dev.comon.toss_watch.feature.dashboard.data.guest

import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary

/**
 * 게스트(더미 데이터 체험) 모드에서 대시보드 화면에 채워 넣는 샘플 계좌/포트폴리오.
 *
 * 실 API 응답 형태를 그대로 흉내 내되, 계좌 2개·통화 혼합·손익 혼합을 갖춰
 * 계좌 선택 다이얼로그와 버블/트리맵 차트가 의미 있게 보이도록 구성한다.
 * [feature.alarm]의 게스트 시드 데이터(`GuestAlarmData`)가 이 종목 코드/종목명을 그대로 재사용한다.
 */
object GuestDashboardData {

    val ACCOUNT_MAIN = Account(
        accountNo = "110-452-198273",
        accountSeq = 1_001L,
        accountType = "BROKERAGE",
    )

    val ACCOUNT_PENSION = Account(
        accountNo = "110-778-604512",
        accountSeq = 1_002L,
        accountType = "BROKERAGE",
    )

    val ACCOUNTS = listOf(ACCOUNT_MAIN, ACCOUNT_PENSION)

    /** [ACCOUNT_MAIN] 보유 종목 — KRW 6종목 + USD 2종목, 수익/손실 혼합. */
    val HOLDINGS_MAIN = listOf(
        HoldingStock(
            stockCode = "005930",
            stockName = "삼성전자",
            currency = Currency.KRW,
            quantity = 30.0,
            averageBuyPrice = 68_000.0,
            totalBuyAmount = 2_040_000.0,
            currentPrice = 82_500.0,
            totalEvaluationAmount = 2_475_000.0,
            profitLoss = 435_000.0,
            returnRate = 21.32,
        ),
        HoldingStock(
            stockCode = "000660",
            stockName = "SK하이닉스",
            currency = Currency.KRW,
            quantity = 10.0,
            averageBuyPrice = 145_000.0,
            totalBuyAmount = 1_450_000.0,
            currentPrice = 198_000.0,
            totalEvaluationAmount = 1_980_000.0,
            profitLoss = 530_000.0,
            returnRate = 36.55,
        ),
        HoldingStock(
            stockCode = "035420",
            stockName = "NAVER",
            currency = Currency.KRW,
            quantity = 15.0,
            averageBuyPrice = 210_000.0,
            totalBuyAmount = 3_150_000.0,
            currentPrice = 195_000.0,
            totalEvaluationAmount = 2_925_000.0,
            profitLoss = -225_000.0,
            returnRate = -7.14,
        ),
        HoldingStock(
            stockCode = "035720",
            stockName = "카카오",
            currency = Currency.KRW,
            quantity = 40.0,
            averageBuyPrice = 52_000.0,
            totalBuyAmount = 2_080_000.0,
            currentPrice = 46_500.0,
            totalEvaluationAmount = 1_860_000.0,
            profitLoss = -220_000.0,
            returnRate = -10.58,
        ),
        HoldingStock(
            stockCode = "373220",
            stockName = "LG에너지솔루션",
            currency = Currency.KRW,
            quantity = 5.0,
            averageBuyPrice = 420_000.0,
            totalBuyAmount = 2_100_000.0,
            currentPrice = 395_000.0,
            totalEvaluationAmount = 1_975_000.0,
            profitLoss = -125_000.0,
            returnRate = -5.95,
        ),
        HoldingStock(
            stockCode = "005380",
            stockName = "현대차",
            currency = Currency.KRW,
            quantity = 8.0,
            averageBuyPrice = 210_000.0,
            totalBuyAmount = 1_680_000.0,
            currentPrice = 245_000.0,
            totalEvaluationAmount = 1_960_000.0,
            profitLoss = 280_000.0,
            returnRate = 16.67,
        ),
        HoldingStock(
            stockCode = "NVDA",
            stockName = "Nvidia Corp",
            currency = Currency.USD,
            quantity = 12.0,
            averageBuyPrice = 620.0,
            totalBuyAmount = 7_440.0,
            currentPrice = 875.28,
            totalEvaluationAmount = 10_503.36,
            profitLoss = 3_063.36,
            returnRate = 41.18,
        ),
        HoldingStock(
            stockCode = "AAPL",
            stockName = "Apple Inc",
            currency = Currency.USD,
            quantity = 20.0,
            averageBuyPrice = 175.0,
            totalBuyAmount = 3_500.0,
            currentPrice = 182.52,
            totalEvaluationAmount = 3_650.4,
            profitLoss = 150.4,
            returnRate = 4.30,
        ),
    )

    /** [ACCOUNT_PENSION] 보유 종목 — KRW 3종목만 담아 종합위탁 계좌와 대비되도록 한다. */
    val HOLDINGS_PENSION = listOf(
        HoldingStock(
            stockCode = "005930",
            stockName = "삼성전자",
            currency = Currency.KRW,
            quantity = 10.0,
            averageBuyPrice = 65_000.0,
            totalBuyAmount = 650_000.0,
            currentPrice = 82_500.0,
            totalEvaluationAmount = 825_000.0,
            profitLoss = 175_000.0,
            returnRate = 26.92,
        ),
        HoldingStock(
            stockCode = "207940",
            stockName = "삼성바이오로직스",
            currency = Currency.KRW,
            quantity = 2.0,
            averageBuyPrice = 850_000.0,
            totalBuyAmount = 1_700_000.0,
            currentPrice = 920_000.0,
            totalEvaluationAmount = 1_840_000.0,
            profitLoss = 140_000.0,
            returnRate = 8.24,
        ),
        HoldingStock(
            stockCode = "005490",
            stockName = "POSCO홀딩스",
            currency = Currency.KRW,
            quantity = 4.0,
            averageBuyPrice = 380_000.0,
            totalBuyAmount = 1_520_000.0,
            currentPrice = 355_000.0,
            totalEvaluationAmount = 1_420_000.0,
            profitLoss = -100_000.0,
            returnRate = -6.58,
        ),
    )

    val PORTFOLIOS: Map<Long, Portfolio> = mapOf(
        ACCOUNT_MAIN.accountSeq to Portfolio(summary = summaryOf(HOLDINGS_MAIN), securities = HOLDINGS_MAIN),
        ACCOUNT_PENSION.accountSeq to Portfolio(summary = summaryOf(HOLDINGS_PENSION), securities = HOLDINGS_PENSION),
    )

    /**
     * 보유 종목 목록으로부터 계좌 요약을 계산한다 — 종목을 수정해도 요약과 어긋나지 않도록
     * 손으로 합계를 적지 않는다.
     *
     * 실 API의 `totalReturnRate`는 토스가 환율을 적용해 계산한 단일값이지만, 더미 데이터는 환율
     * 정보가 없어 KRW 종목 기준 수익률로 근사한다(KRW 종목이 없는 계좌라면 USD 기준으로 대체).
     */
    private fun summaryOf(holdings: List<HoldingStock>): PortfolioSummary {
        val krw = holdings.filter { it.currency == Currency.KRW }
        val usd = holdings.filter { it.currency == Currency.USD }

        val investmentKrw = krw.sumOf { it.totalBuyAmount }
        val evaluationKrw = krw.sumOf { it.totalEvaluationAmount }
        val investmentUsd = usd.sumOf { it.totalBuyAmount }
        val evaluationUsd = usd.sumOf { it.totalEvaluationAmount }
        val profitLossKrw = evaluationKrw - investmentKrw
        val profitLossUsd = evaluationUsd - investmentUsd

        val returnRate = when {
            investmentKrw > 0.0 -> profitLossKrw / investmentKrw * 100
            investmentUsd > 0.0 -> profitLossUsd / investmentUsd * 100
            else -> 0.0
        }

        return PortfolioSummary(
            totalInvestmentKrw = investmentKrw,
            totalInvestmentUsd = investmentUsd,
            totalEvaluationKrw = evaluationKrw,
            totalEvaluationUsd = evaluationUsd,
            totalProfitLossKrw = profitLossKrw,
            totalProfitLossUsd = profitLossUsd,
            totalReturnRate = returnRate,
        )
    }
}
