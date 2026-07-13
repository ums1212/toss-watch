package dev.comon.toss_watch.feature.dashboard.data.remote.dto

import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAssetsResponse(
    @SerialName("total_assets") val totalAssets: Long,
    @SerialName("total_profit") val totalProfit: Long = 0L,
    @SerialName("profit_rate") val profitRate: Double = 0.0,
)

fun UserAssetsResponse.toUserAssets(): UserAssets =
    UserAssets(
        totalAssets = totalAssets,
        totalProfit = totalProfit,
        profitRate = profitRate,
    )
