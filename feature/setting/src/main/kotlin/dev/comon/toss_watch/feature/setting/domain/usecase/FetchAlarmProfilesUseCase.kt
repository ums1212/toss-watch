package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/** 등록된 알림 스케줄 프로필 목록 조회. */
class FetchAlarmProfilesUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(): NetworkResult<List<AlarmProfile>> =
        settingRepository.fetchAlarmProfiles()
}
