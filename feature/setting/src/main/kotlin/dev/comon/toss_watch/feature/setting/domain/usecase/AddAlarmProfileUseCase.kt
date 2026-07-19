package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/** 새 알림 프로필 등록 (stock_code + alarm_time → POST /api/v1/toss-watch/notifications/). */
class AddAlarmProfileUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(
        stockCode: String,
        hour: Int,
        minute: Int,
    ): NetworkResult<AlarmProfile> =
        settingRepository.addAlarmProfile(stockCode = stockCode, hour = hour, minute = minute)
}
