package dev.comon.watch_app.presentation.setting

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.watch_app.domain.usecase.GetCustomAlarmImagePathsUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

/** @property customImagePaths 폰에서 설정한 사용자 지정 알람 이미지의 파일 경로. 없는 슬롯은 기본 이미지를 쓴다. */
data class WatchAlarmImagePreviewUiState(
    val customImagePaths: Map<WatchAlarmImageSlot, String> = emptyMap(),
) : UiState

sealed interface WatchAlarmImagePreviewIntent : UiIntent {
    /** 워치에 저장된 이미지를 다시 읽는다 — 화면을 띄워 둔 채 폰에서 이미지를 바꿨을 때. */
    data object Reload : WatchAlarmImagePreviewIntent
}

sealed interface WatchAlarmImagePreviewEffect : UiSideEffect

/** 디버그 빌드 전용 알람 이미지 미리보기. 알람을 기다리지 않고 현재 설정된 이미지를 확인한다. */
@HiltViewModel
class WatchAlarmImagePreviewViewModel @Inject constructor(
    private val getCustomAlarmImagePaths: GetCustomAlarmImagePathsUseCase,
) : BaseMviViewModel<WatchAlarmImagePreviewUiState, WatchAlarmImagePreviewIntent, WatchAlarmImagePreviewEffect>(
    WatchAlarmImagePreviewUiState(),
) {
    init {
        load()
    }

    override fun handleIntent(intent: WatchAlarmImagePreviewIntent) {
        when (intent) {
            WatchAlarmImagePreviewIntent.Reload -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val paths = getCustomAlarmImagePaths()
            updateState { copy(customImagePaths = paths) }
        }
    }
}
