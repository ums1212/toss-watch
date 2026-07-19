package dev.comon.watch_app.domain.usecase

import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject

/**
 * 현재 FCM 등록 토큰을 가져온다.
 * [forceRefresh]가 true면 기존 토큰을 폐기하고 새로 발급받는다("QR 새로고침").
 */
class GetFcmTokenUseCase @Inject constructor(
    private val watchPairingRepository: WatchPairingRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<String> =
        if (forceRefresh) {
            watchPairingRepository.refreshFcmToken()
        } else {
            watchPairingRepository.getFcmToken()
        }
}
