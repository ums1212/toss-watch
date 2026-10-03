package dev.comon.toss_watch.feature.setting.domain.model

import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot

/**
 * 워치 알람 이미지 한 슬롯의 현재 설정.
 *
 * @property customPath 사용자가 설정한 이미지 파일의 절대 경로. `null`이면 기본 이미지를 쓴다.
 * @property updatedAt 이미지 파일의 최종 수정 시각(epoch millis) — 같은 경로에 덮어써도 변경을 감지하기 위한 값.
 */
data class AlarmImage(
    val slot: WatchAlarmImageSlot,
    val customPath: String? = null,
    val updatedAt: Long = 0,
)

/** 원본 이미지에서 잘라낼 영역. 각 값은 원본 가로/세로에 대한 0..1 비율이다. */
data class AlarmImageCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
