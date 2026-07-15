package dev.comon.toss_watch.feature.tosskey.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import javax.inject.Inject

/**
 * 토스증권 Open API 키 등록 유스케이스.
 * Presentation 레이어는 Repository에 직접 접근하지 않고 반드시 이 클래스를 거친다.
 */
class RegisterTossKeyUseCase @Inject constructor(
    private val tossKeyRepository: TossKeyRepository,
) {

    suspend operator fun invoke(clientId: String, clientSecret: String): NetworkResult<Unit> =
        tossKeyRepository.registerTossKey(clientId, clientSecret)
}
