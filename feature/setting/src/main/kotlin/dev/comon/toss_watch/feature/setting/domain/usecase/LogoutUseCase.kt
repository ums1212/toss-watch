package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/**
 * 발급받은 세션 토큰을 로컬에서 제거한다.
 * :app 최상위 라우터가 세션 소멸을 감지해 로그인 화면으로 자동 전환한다.
 */
class LogoutUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    operator fun invoke() = settingRepository.logout()
}
