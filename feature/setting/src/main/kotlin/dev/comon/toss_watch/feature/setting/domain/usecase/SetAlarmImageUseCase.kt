package dev.comon.toss_watch.feature.setting.domain.usecase

import android.graphics.Bitmap
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import dev.comon.toss_watch.feature.setting.domain.repository.AlarmImageRepository
import javax.inject.Inject

/** 편집한 크롭 영역으로 슬롯의 알람 이미지를 교체한다. 저장에 실패하면 `false`. */
class SetAlarmImageUseCase @Inject constructor(
    private val alarmImageRepository: AlarmImageRepository,
) {
    suspend operator fun invoke(slot: WatchAlarmImageSlot, source: Bitmap, crop: AlarmImageCrop): Boolean =
        alarmImageRepository.set(slot, source, crop)
}
