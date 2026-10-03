package dev.comon.toss_watch.feature.setting.presentation.alarmimage

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import dev.comon.toss_watch.feature.setting.domain.usecase.DecodeAlarmImageUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObserveAlarmImagesUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ResetAlarmImageUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.SetAlarmImageUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmImageViewModel @Inject constructor(
    private val observeAlarmImagesUseCase: ObserveAlarmImagesUseCase,
    private val observePairedWatchUseCase: ObservePairedWatchUseCase,
    private val decodeAlarmImageUseCase: DecodeAlarmImageUseCase,
    private val setAlarmImageUseCase: SetAlarmImageUseCase,
    private val resetAlarmImageUseCase: ResetAlarmImageUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<AlarmImageUiState, AlarmImageUiIntent, AlarmImageUiSideEffect>(AlarmImageUiState()) {

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            observeAlarmImagesUseCase().collect { images -> updateState { copy(images = images) } }
        }
        viewModelScope.launch(dispatcherProvider.io) {
            observePairedWatchUseCase().collect { pairedWatch ->
                updateState { copy(isWatchPaired = pairedWatch != null) }
            }
        }
    }

    override fun handleIntent(intent: AlarmImageUiIntent) {
        when (intent) {
            AlarmImageUiIntent.OnBackClicked ->
                if (uiState.value.editing != null) cancelEditing()
                else sendSideEffect(AlarmImageUiSideEffect.NavigateBack)

            is AlarmImageUiIntent.OnImagePicked -> startEditing(intent.slot, intent.uri)
            is AlarmImageUiIntent.OnCropConfirmed -> save(intent.crop)
            AlarmImageUiIntent.OnCropCancelled -> cancelEditing()
            is AlarmImageUiIntent.OnResetClicked -> reset(intent.slot)
        }
    }

    private fun startEditing(slot: WatchAlarmImageSlot, uri: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            val source = decodeAlarmImageUseCase(uri)
            if (source == null) {
                sendSideEffect(AlarmImageUiSideEffect.ShowLoadFailed)
            } else {
                updateState { copy(editing = AlarmImageEditing(slot, source)) }
            }
        }
    }

    private fun save(crop: AlarmImageCrop) {
        val editing = uiState.value.editing ?: return
        if (uiState.value.isSaving) return
        updateState { copy(isSaving = true) }
        viewModelScope.launch(dispatcherProvider.io) {
            val saved = setAlarmImageUseCase(editing.slot, editing.source, crop)
            // 실패하면 편집 화면에 그대로 남겨 다시 시도할 수 있게 한다.
            updateState { copy(isSaving = false, editing = if (saved) null else this.editing) }
            if (!saved) sendSideEffect(AlarmImageUiSideEffect.ShowSaveFailed)
        }
    }

    private fun cancelEditing() {
        if (uiState.value.isSaving) return
        updateState { copy(editing = null) }
    }

    private fun reset(slot: WatchAlarmImageSlot) {
        viewModelScope.launch(dispatcherProvider.io) {
            resetAlarmImageUseCase(slot)
        }
    }
}
