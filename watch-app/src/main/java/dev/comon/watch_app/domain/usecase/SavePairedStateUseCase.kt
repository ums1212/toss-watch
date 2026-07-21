package dev.comon.watch_app.domain.usecase

import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject

/** 연동 완료 여부를 로컬에 영속화한다. */
class SavePairedStateUseCase @Inject constructor(
    private val watchPairingRepository: WatchPairingRepository,
) {
    suspend operator fun invoke(paired: Boolean) = watchPairingRepository.setPaired(paired)
}
