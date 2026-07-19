package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 연동 완료된 워치(기기명+UUID)를 구독한다. 미연동이면 `null`을 방출한다. */
class ObservePairedWatchUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    operator fun invoke(): Flow<PairedWatchInfo?> = settingRepository.observePairedWatch()
}
