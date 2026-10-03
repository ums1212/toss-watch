package dev.comon.toss_watch.feature.setting.presentation.alarmimage

import android.graphics.Bitmap
import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop

data class AlarmImageUiState(
    /** 상승/보합/하락 슬롯별 현재 이미지. */
    val images: List<AlarmImage> = WatchAlarmImageSlot.entries.map { AlarmImage(it) },

    /** false면 "워치를 연동하면 전송돼요" 안내를 노출한다. 설정 자체는 미연동 상태에서도 가능하다. */
    val isWatchPaired: Boolean = true,

    /** `null`이 아니면 목록 대신 크롭 편집 화면을 띄운다. */
    val editing: AlarmImageEditing? = null,

    /** 크롭 결과를 저장하는 중 — 적용 버튼을 스피너로 바꾸고 중복 저장을 막는다. */
    val isSaving: Boolean = false,
) : UiState

/** 이미지 피커에서 고른 뒤 아직 적용하지 않은 편집 중인 이미지. */
data class AlarmImageEditing(
    val slot: WatchAlarmImageSlot,
    val source: Bitmap,
)

sealed interface AlarmImageUiIntent : UiIntent {

    /** 상단 앱바의 뒤로가기. 편집 중이면 편집만 취소한다. */
    data object OnBackClicked : AlarmImageUiIntent

    /** 이미지 피커에서 [slot]에 넣을 이미지를 골랐다. */
    data class OnImagePicked(val slot: WatchAlarmImageSlot, val uri: String) : AlarmImageUiIntent

    /** 크롭 편집의 "적용" 버튼. */
    data class OnCropConfirmed(val crop: AlarmImageCrop) : AlarmImageUiIntent

    /** 크롭 편집의 "취소" 버튼 또는 시스템 뒤로가기. */
    data object OnCropCancelled : AlarmImageUiIntent

    /** "기본 이미지로 되돌리기" 버튼. */
    data class OnResetClicked(val slot: WatchAlarmImageSlot) : AlarmImageUiIntent
}

sealed interface AlarmImageUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 백스택을 pop한다. */
    data object NavigateBack : AlarmImageUiSideEffect

    /** 고른 이미지를 읽지 못했다. */
    data object ShowLoadFailed : AlarmImageUiSideEffect

    /** 크롭한 이미지를 저장하지 못했다. */
    data object ShowSaveFailed : AlarmImageUiSideEffect
}
