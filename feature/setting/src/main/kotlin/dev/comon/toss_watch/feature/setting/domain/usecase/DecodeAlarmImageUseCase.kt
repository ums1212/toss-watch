package dev.comon.toss_watch.feature.setting.domain.usecase

import android.graphics.Bitmap
import dev.comon.toss_watch.feature.setting.domain.repository.AlarmImageRepository
import javax.inject.Inject

/** 이미지 피커에서 고른 이미지를 크롭 편집용 비트맵으로 읽는다. 읽을 수 없으면 `null`. */
class DecodeAlarmImageUseCase @Inject constructor(
    private val alarmImageRepository: AlarmImageRepository,
) {
    suspend operator fun invoke(sourceUri: String): Bitmap? = alarmImageRepository.decode(sourceUri)
}
