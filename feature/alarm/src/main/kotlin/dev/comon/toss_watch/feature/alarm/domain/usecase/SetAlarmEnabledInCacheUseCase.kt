package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

/**
 * 캐시에만 활성 여부를 즉시 반영 — 토글 스위치를 누르는 즉시 UI가 움직이는 낙관적 업데이트,
 * 그리고 서버 요청 실패 시 이전 값으로 되돌리는 롤백 양쪽에 쓰인다.
 */
class SetAlarmEnabledInCacheUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    operator fun invoke(alarmId: Long, isEnabled: Boolean) {
        alarmRepository.setAlarmEnabledInCache(alarmId, isEnabled)
    }
}
