package dev.comon.watch_app.presentation.onboarding

import android.graphics.Bitmap
import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState

data class WatchOnboardingUiState(
    val isLoading: Boolean = true,
    val qrBitmap: Bitmap? = null,
    val errorMessage: String? = null,
) : UiState

sealed interface WatchOnboardingUiIntent : UiIntent {
    data object LoadToken : WatchOnboardingUiIntent
    data object RetryClicked : WatchOnboardingUiIntent
}

sealed interface WatchOnboardingUiSideEffect : UiSideEffect
