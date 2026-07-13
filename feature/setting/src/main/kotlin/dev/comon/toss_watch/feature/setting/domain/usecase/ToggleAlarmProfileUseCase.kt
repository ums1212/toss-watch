package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/** 알림 프로필 활성/비활성 토글 (PUT /api/v1/notifications/{id}/). */
class ToggleAlarmProfileUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> =
        settingRepository.updateAlarmProfile(alarmId = alarmId, isEnabled = isEnabled)
}
