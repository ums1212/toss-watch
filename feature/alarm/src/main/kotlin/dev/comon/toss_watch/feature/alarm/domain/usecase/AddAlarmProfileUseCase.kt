package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

/** 새 알림 프로필 등록 (stock_code + stock_name + alarm_time + days_of_week → POST /api/v1/toss-watch/notifications/). */
class AddAlarmProfileUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke(
        stockCode: String,
        stockName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile> =
        alarmRepository.addAlarmProfile(
            stockCode = stockCode,
            stockName = stockName,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
        )
}
