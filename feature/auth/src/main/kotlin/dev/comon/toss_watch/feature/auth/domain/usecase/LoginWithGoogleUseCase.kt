package dev.comon.toss_watch.feature.auth.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.domain.model.UserSession
import dev.comon.toss_watch.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 구글 소셜 로그인 유스케이스.
 * Presentation 레이어는 Repository에 직접 접근하지 않고 반드시 이 클래스를 거친다.
 */
class LoginWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {

    suspend operator fun invoke(idToken: String): NetworkResult<UserSession> =
        authRepository.loginWithGoogle(idToken)
}
