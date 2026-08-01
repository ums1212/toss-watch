package dev.comon.toss_watch.feature.auth.domain.usecase

import dev.comon.toss_watch.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 로그인 화면의 "게스트로 둘러보기" 유스케이스.
 * Presentation 레이어는 Repository에 직접 접근하지 않고 반드시 이 클래스를 거친다.
 */
class EnterGuestModeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.enterGuestMode()
}
