package dev.comon.toss_watch.feature.dashboard.domain.model

/**
 * 계좌 자산 요약 (원화 기준).
 *
 * @param totalAssets 총 평가 자산
 * @param totalProfit 총 평가 손익 (음수 가능)
 * @param profitRate 총 수익률(%) — 예: +3.42 → 3.42
 */
data class UserAssets(
    val totalAssets: Long,
    val totalProfit: Long,
    val profitRate: Double,
)
