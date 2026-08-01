package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 게스트(더미 데이터 체험) 모드 여부를 구독한다 — 설정 화면이 로그아웃/로그인 전환 UI를 고르는 데 쓴다. */
class ObserveGuestModeUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    operator fun invoke(): Flow<Boolean> = settingRepository.observeGuestMode()
}
