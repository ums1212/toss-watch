package dev.comon.toss_watch.feature.dashboard.domain.model

/**
 * 유저가 토스증권에 보유한 계좌.
 *
 * @param accountNo 계좌번호
 * @param accountSeq 계좌 식별 키 — 포트폴리오 등 다른 토스 API 호출 시 식별자로 사용
 * @param accountType 계좌 유형 (예: "BROKERAGE")
 */
data class Account(
    val accountNo: String,
    val accountSeq: Long,
    val accountType: String,
)
