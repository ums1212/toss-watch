package dev.comon.toss_watch.feature.dashboard.domain.model

/** 계좌 통화. 서버가 내려주는 값 외의 미지원 값은 [KRW]로 취급한다. */
enum class Currency {
    KRW,
    USD,
}

fun String.toCurrency(): Currency =
    when (this) {
        "USD" -> Currency.USD
        else -> Currency.KRW
    }

/**
 * 계좌 포트폴리오 — 통화별 합계 요약과 보유 종목 상세.
 *
 * 계좌당 KRW/USD 종목이 섞일 수 있어 [summary]의 합계는 통화별로 분리되어 있다.
 */
data class Portfolio(
    val summary: PortfolioSummary,
    val securities: List<HoldingStock>,
)

/**
 * @param totalInvestmentKrw 총 매수 금액 중 KRW 종목 합계
 * @param totalInvestmentUsd 총 매수 금액 중 USD 종목 합계
 * @param totalEvaluationKrw 총 평가 금액 중 KRW 종목 합계
 * @param totalEvaluationUsd 총 평가 금액 중 USD 종목 합계
 * @param totalProfitLossKrw 총 평가 손익 중 KRW 종목 합계
 * @param totalProfitLossUsd 총 평가 손익 중 USD 종목 합계
 * @param totalReturnRate 계좌 전체 수익률(%) — 토스가 통화 환산해 계산한 단일값
 */
data class PortfolioSummary(
    val totalInvestmentKrw: Double,
    val totalInvestmentUsd: Double,
    val totalEvaluationKrw: Double,
    val totalEvaluationUsd: Double,
    val totalProfitLossKrw: Double,
    val totalProfitLossUsd: Double,
    val totalReturnRate: Double,
)

/**
 * 보유 종목 1건. 금액 필드들은 전부 [currency] 기준.
 *
 * @param stockCode 종목 코드/티커
 * @param stockName 종목명
 * @param quantity 보유 수량
 * @param averageBuyPrice 평균 매수 단가
 * @param totalBuyAmount 매수 금액
 * @param currentPrice 현재가
 * @param totalEvaluationAmount 평가 금액
 * @param profitLoss 평가 손익
 * @param returnRate 종목별 수익률(%)
 */
data class HoldingStock(
    val stockCode: String,
    val stockName: String,
    val currency: Currency,
    val quantity: Double,
    val averageBuyPrice: Double,
    val totalBuyAmount: Double,
    val currentPrice: Double,
    val totalEvaluationAmount: Double,
    val profitLoss: Double,
    val returnRate: Double,
)
