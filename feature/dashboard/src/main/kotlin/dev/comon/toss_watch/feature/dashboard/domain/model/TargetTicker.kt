package dev.comon.toss_watch.feature.dashboard.domain.model

/**
 * 사용자가 알림 대상으로 등록한 관찰 종목.
 *
 * @param code 종목 코드 (예: "005930")
 * @param name 종목명 (예: "삼성전자")
 * @param currentPrice 현재가 (원)
 * @param changeRate 전일 대비 등락률(%) — 상승 양수 / 하락 음수
 */
data class TargetTicker(
    val code: String,
    val name: String,
    val currentPrice: Long,
    val changeRate: Double,
)
