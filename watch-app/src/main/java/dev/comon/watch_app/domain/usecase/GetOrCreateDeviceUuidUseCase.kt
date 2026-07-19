package dev.comon.watch_app.domain.usecase

import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject

/** 워치 기기 UUID를 조회하되, 최초 실행이면 발행해 영속화한다. */
class GetOrCreateDeviceUuidUseCase @Inject constructor(
    private val watchPairingRepository: WatchPairingRepository,
) {
    suspend operator fun invoke(): String = watchPairingRepository.getOrCreateDeviceUuid()
}
