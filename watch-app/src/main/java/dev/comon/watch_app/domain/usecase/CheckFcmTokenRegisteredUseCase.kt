package dev.comon.watch_app.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject

/** 2-5. 이 FCM 토큰이 서버에 이미 등록(연동 완료)됐는지 확인한다. */
class CheckFcmTokenRegisteredUseCase @Inject constructor(
    private val watchPairingRepository: WatchPairingRepository,
) {
    suspend operator fun invoke(fcmToken: String): NetworkResult<Boolean> =
        watchPairingRepository.checkFcmTokenRegistered(fcmToken)
}
