package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/** 알림 프로필 삭제 (DELETE /api/v1/toss-watch/notifications/{id}/). */
class DeleteAlarmProfileUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(alarmId: Long): NetworkResult<Unit> =
        settingRepository.deleteAlarmProfile(alarmId)
}
