package dev.comon.toss_watch.feature.dashboard.data.remote.dto

import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import dev.comon.toss_watch.feature.dashboard.domain.model.toCurrency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioResponse(
    @SerialName("summary") val summary: PortfolioSummaryDto,
    @SerialName("securities") val securities: List<SecurityDto>,
)

@Serializable
data class PortfolioSummaryDto(
    @SerialName("total_investment_krw") val totalInvestmentKrw: Double = 0.0,
    @SerialName("total_investment_usd") val totalInvestmentUsd: Double = 0.0,
    @SerialName("total_evaluation_krw") val totalEvaluationKrw: Double = 0.0,
    @SerialName("total_evaluation_usd") val totalEvaluationUsd: Double = 0.0,
    @SerialName("total_profit_loss_krw") val totalProfitLossKrw: Double = 0.0,
    @SerialName("total_profit_loss_usd") val totalProfitLossUsd: Double = 0.0,
    // 토스가 통화 환산해 계산한 단일값. 소수 2자리 문자열("26.92")로 내려온다.
    @SerialName("total_return_rate") val totalReturnRate: String = "0",
)

@Serializable
data class SecurityDto(
    @SerialName("stock_code") val stockCode: String,
    @SerialName("stock_name") val stockName: String,
    @SerialName("currency") val currency: String,
    @SerialName("quantity") val quantity: Double,
    @SerialName("average_buy_price") val averageBuyPrice: Double,
    @SerialName("total_buy_amount") val totalBuyAmount: Double,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("total_evaluation_amount") val totalEvaluationAmount: Double,
    @SerialName("profit_loss") val profitLoss: Double,
    // 종목별 수익률(%), 소수 2자리 문자열("11.54")로 내려온다.
    @SerialName("return_rate") val returnRate: String = "0",
)

fun PortfolioResponse.toPortfolio(): Portfolio =
    Portfolio(
        summary = summary.toPortfolioSummary(),
        securities = securities.map { it.toHoldingStock() },
    )

private fun PortfolioSummaryDto.toPortfolioSummary(): PortfolioSummary =
    PortfolioSummary(
        totalInvestmentKrw = totalInvestmentKrw,
        totalInvestmentUsd = totalInvestmentUsd,
        totalEvaluationKrw = totalEvaluationKrw,
        totalEvaluationUsd = totalEvaluationUsd,
        totalProfitLossKrw = totalProfitLossKrw,
        totalProfitLossUsd = totalProfitLossUsd,
        totalReturnRate = totalReturnRate.toDoubleOrNull() ?: 0.0,
    )

private fun SecurityDto.toHoldingStock(): HoldingStock =
    HoldingStock(
        stockCode = stockCode,
        stockName = stockName,
        currency = currency.toCurrency(),
        quantity = quantity,
        averageBuyPrice = averageBuyPrice,
        totalBuyAmount = totalBuyAmount,
        currentPrice = currentPrice,
        totalEvaluationAmount = totalEvaluationAmount,
        profitLoss = profitLoss,
        returnRate = returnRate.toDoubleOrNull() ?: 0.0,
    )
