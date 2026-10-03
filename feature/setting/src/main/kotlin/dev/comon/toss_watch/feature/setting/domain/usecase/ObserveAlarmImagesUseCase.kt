package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.domain.repository.AlarmImageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 워치 알람 이미지(상승/보합/하락)의 현재 설정을 구독한다. */
class ObserveAlarmImagesUseCase @Inject constructor(
    private val alarmImageRepository: AlarmImageRepository,
) {
    operator fun invoke(): Flow<List<AlarmImage>> = alarmImageRepository.observe()
}
