package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.domain.repository.AlarmImageRepository
import javax.inject.Inject

/** 슬롯의 알람 이미지를 기본 이미지로 되돌린다. */
class ResetAlarmImageUseCase @Inject constructor(
    private val alarmImageRepository: AlarmImageRepository,
) {
    suspend operator fun invoke(slot: WatchAlarmImageSlot) = alarmImageRepository.reset(slot)
}
