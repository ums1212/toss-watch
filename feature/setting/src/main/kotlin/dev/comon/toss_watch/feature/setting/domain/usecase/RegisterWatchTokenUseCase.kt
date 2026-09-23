package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/**
 * Wear OS 워치의 연동 정보(FCM 토큰+UUID+모델명)를 서버 계정에 등록.
 * FCM 토큰은 기기 식별용이며, 알람은 워치가 동기화된 목록으로 로컬 AlarmManager에서 울린다.
 */
class RegisterWatchTokenUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(fcmToken: String, uuid: String, modelName: String): NetworkResult<Unit> =
        settingRepository.registerWatchToken(fcmToken = fcmToken, uuid = uuid, modelName = modelName)
}
