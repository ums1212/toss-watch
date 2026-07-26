package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

/** 서버에서 알림 프로필 목록을 다시 조회해 캐시를 갈아끼운다. */
class FetchAlarmProfilesUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke(): NetworkResult<Unit> = alarmRepository.refreshAlarmProfiles()
}
