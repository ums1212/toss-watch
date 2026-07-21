package dev.comon.watch_app.domain.usecase

import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject

/** 로컬에 저장된 연동 완료 여부를 조회한다. */
class IsPairedUseCase @Inject constructor(
    private val watchPairingRepository: WatchPairingRepository,
) {
    suspend operator fun invoke(): Boolean = watchPairingRepository.isPaired()
}
