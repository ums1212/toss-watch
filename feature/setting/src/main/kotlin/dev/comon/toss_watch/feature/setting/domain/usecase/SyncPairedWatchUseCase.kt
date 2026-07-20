package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject

/**
 * 서버 워치 FCM 토큰 등록 상태를 재조회해 로컬(core:datastore) pairedWatch를 복원/정리한다.
 * 폰앱 재설치 등으로 로컬 값이 유실됐을 때 서버 상태로 되돌리기 위한 best-effort 동기화.
 */
class SyncPairedWatchUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(): NetworkResult<Unit> = settingRepository.syncPairedWatch()
}
