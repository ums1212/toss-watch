package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.feature.alarm.domain.model.AlarmAddViolation
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 새 알림을 등록해도 되는지 공유 캐시 기준으로 검사한다 — 문제가 없으면 null.
 * 폰 화면과 워치 추가 경로가 같은 규칙을 쓰도록 [AddAlarmProfileUseCase] 호출 전에 거친다.
 */
class ValidateAlarmAddUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke(
        stockCode: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): AlarmAddViolation? {
        val alarms = alarmRepository.observeAlarmProfiles().first()
        return when {
            alarms.size >= AlarmProfile.MAX_COUNT -> AlarmAddViolation.LIMIT_REACHED
            alarms.any { it.conflictsWith(stockCode, hour, minute, daysOfWeek) } -> AlarmAddViolation.DUPLICATE
            else -> null
        }
    }
}
