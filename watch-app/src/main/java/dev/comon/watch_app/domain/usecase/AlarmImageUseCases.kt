package dev.comon.watch_app.domain.usecase

import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.watch_app.domain.repository.AlarmImageRepository
import javax.inject.Inject

/** 알람 이미지 Scene에서 기본 이미지 대신 띄울 사용자 지정 이미지 경로를 슬롯별로 가져온다. */
class GetCustomAlarmImagePathsUseCase @Inject constructor(private val images: AlarmImageRepository) {
    suspend operator fun invoke(): Map<WatchAlarmImageSlot, String> = images.customImagePaths()
}
