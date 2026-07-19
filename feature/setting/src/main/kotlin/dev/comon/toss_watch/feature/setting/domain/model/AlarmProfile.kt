package dev.comon.toss_watch.feature.setting.domain.model

/**
 * 종목별 알림 스케줄 프로필 — Django 스케줄러(cron)가 이 설정대로 FCM을 발사한다.
 *
 * @param id 서버 리소스 ID
 * @param stockCode 종목 코드 (예: "005930")
 * @param stockName 종목명
 * @param hour 알림 시각(0-23)
 * @param minute 알림 분(0-59)
 * @param isEnabled 알림 활성 여부
 * @param disabledReason 서버가 자동 비활성화한 경우 그 사유 (예: 워치 FCM 토큰 미등록, 토스 키 미등록)
 */
data class AlarmProfile(
    val id: Long,
    val stockCode: String,
    val stockName: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val disabledReason: String = "",
)
