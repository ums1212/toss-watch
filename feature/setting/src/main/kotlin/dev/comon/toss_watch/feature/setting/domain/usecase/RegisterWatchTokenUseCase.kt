package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/** Wear OS 컴패니언의 FCM 토큰을 Django 알림 인프라에 등록. */
class RegisterWatchTokenUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(fcmToken: String): NetworkResult<Unit> =
        settingRepository.registerWatchToken(fcmToken)
}
