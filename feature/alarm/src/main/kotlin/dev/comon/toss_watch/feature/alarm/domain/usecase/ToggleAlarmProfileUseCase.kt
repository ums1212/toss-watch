package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

/** 알림 프로필 활성/비활성 토글 (PATCH /api/v1/toss-watch/notifications/{id}/). */
class ToggleAlarmProfileUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile> =
        alarmRepository.updateAlarmProfile(alarmId = alarmId, isEnabled = isEnabled)
}
