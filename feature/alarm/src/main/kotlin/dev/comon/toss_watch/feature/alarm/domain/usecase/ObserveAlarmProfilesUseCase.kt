package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 등록된 알림 프로필 전체를 구독 — AlarmScreen/AlarmDetailScreen이 같은 캐시를 관측한다. */
class ObserveAlarmProfilesUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    operator fun invoke(): Flow<List<AlarmProfile>> = alarmRepository.observeAlarmProfiles()
}
