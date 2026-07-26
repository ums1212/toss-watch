package dev.comon.toss_watch.feature.setting.data.remote.dto

import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알림 프로필 생성 요청 — alarm_time은 "HH:mm" (서버 TimeField 계약).
 * daysOfWeek: 0(월)~6(일) (Python `date.weekday()` 기준). 서버는 빈 배열/범위 밖 값을 400으로 거부한다.
 */
@Serializable
data class AlarmProfileRequest(
    @SerialName("stock_code") val stockCode: String,
    @SerialName("alarm_time") val alarmTime: String,
    @SerialName("days_of_week") val daysOfWeek: List<Int>,
)

@Serializable
data class AlarmToggleRequest(
    @SerialName("is_active") val isEnabled: Boolean,
)

@Serializable
data class AlarmProfileResponse(
    @SerialName("id") val id: Long,
    @SerialName("stock_code") val stockCode: String,
    @SerialName("stock_name") val stockName: String = "",
    @SerialName("alarm_time") val alarmTime: String,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
    @SerialName("is_active") val isEnabled: Boolean = true,
    @SerialName("disabled_reason") val disabledReason: String = "",
)

fun AlarmProfileResponse.toAlarmProfile(): AlarmProfile {
    // "HH:mm" 또는 "HH:mm:ss" 모두 수용한다.
    val parts = alarmTime.split(":")
    return AlarmProfile(
        id = id,
        stockCode = stockCode,
        stockName = stockName.ifBlank { stockCode },
        hour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
        minute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        disabledReason = disabledReason,
    )
}

fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
