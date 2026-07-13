package dev.comon.toss_watch.feature.dashboard.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchTargetTickersUseCase
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchUserAssetsUseCase
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val fetchUserAssetsUseCase: FetchUserAssetsUseCase,
    private val fetchTargetTickersUseCase: FetchTargetTickersUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<DashboardUiState, DashboardUiIntent, DashboardUiSideEffect>(
    DashboardUiState(),
) {

    init {
        load(isRefresh = false)
    }

    override fun handleIntent(intent: DashboardUiIntent) {
        when (intent) {
            DashboardUiIntent.OnRefreshTriggered -> load(isRefresh = true)

            DashboardUiIntent.OnSettingClicked ->
                sendSideEffect(DashboardUiSideEffect.NavigateToSetting)

            DashboardUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    private fun load(isRefresh: Boolean) {
        val current = uiState.value
        if (current.isLoading || current.isRefreshing) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState {
                copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    errorMessage = null,
                )
            }

            // 자산 요약과 종목 시세는 독립 엔드포인트 — 병렬 조회로 체감 로딩을 줄인다.
            val (assetsResult, tickersResult) = coroutineScope {
                val assets = async { fetchUserAssetsUseCase() }
                val tickers = async { fetchTargetTickersUseCase() }
                assets.await() to tickers.await()
            }

            updateState {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    totalAssets = (assetsResult as? NetworkResult.Success)?.data ?: totalAssets,
                    activeTickers = (tickersResult as? NetworkResult.Success)?.data
                        ?: activeTickers,
                    errorMessage = firstErrorMessage(assetsResult, tickersResult),
                )
            }
        }
    }

    private fun firstErrorMessage(vararg results: NetworkResult<*>): String? =
        results.firstNotNullOfOrNull { result ->
            when (result) {
                is NetworkResult.Success -> null
                is NetworkResult.ApiError -> result.message ?: DEFAULT_API_ERROR
                is NetworkResult.NetworkError -> DEFAULT_NETWORK_ERROR
            }
        }

    companion object {
        const val DEFAULT_API_ERROR = "대시보드 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_NETWORK_ERROR = "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
    }
}
