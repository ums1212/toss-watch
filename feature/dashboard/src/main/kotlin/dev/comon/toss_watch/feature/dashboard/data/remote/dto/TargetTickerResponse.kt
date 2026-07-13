package dev.comon.toss_watch.feature.dashboard.data.remote.dto

import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TargetTickerResponse(
    @SerialName("stock_code") val stockCode: String,
    @SerialName("stock_name") val stockName: String,
    @SerialName("current_price") val currentPrice: Long,
    @SerialName("change_rate") val changeRate: Double = 0.0,
)

fun TargetTickerResponse.toTargetTicker(): TargetTicker =
    TargetTicker(
        code = stockCode,
        name = stockName,
        currentPrice = currentPrice,
        changeRate = changeRate,
    )
