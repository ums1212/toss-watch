package dev.comon.toss_watch.feature.dashboard.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchAccountsUseCase
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchPortfolioUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val fetchAccountsUseCase: FetchAccountsUseCase,
    private val fetchPortfolioUseCase: FetchPortfolioUseCase,
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

            is DashboardUiIntent.OnAccountSelected -> selectAccount(intent.accountSeq)

            DashboardUiIntent.OnSettingClicked ->
                sendSideEffect(DashboardUiSideEffect.NavigateToSetting)

            DashboardUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    /**
     * 대시보드 진입/새로고침 흐름 — 계좌목록을 먼저 조회한 뒤,
     * 이전에 선택된 계좌가 목록에 남아 있으면 그 계좌를, 아니면 첫 번째 계좌를 골라
     * 해당 계좌의 포트폴리오를 조회한다.
     */
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

            val previousSelected = uiState.value.selectedAccountSeq
            val accountsResult = fetchAccountsUseCase()
            val fetchedAccounts = (accountsResult as? NetworkResult.Success)?.data

            val targetAccountSeq = when {
                fetchedAccounts == null -> previousSelected
                fetchedAccounts.any { it.accountSeq == previousSelected } -> previousSelected
                else -> fetchedAccounts.firstOrNull()?.accountSeq
            }

            val portfolioResult = fetchPortfolioUseCase(targetAccountSeq)
            val portfolioSuccess = portfolioResult as? NetworkResult.Success

            updateState {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    accounts = fetchedAccounts ?: accounts,
                    // 포트폴리오 조회에 실패하면 화면엔 이전 계좌 데이터가 그대로 남으므로,
                    // selectedAccountSeq도 이전 값을 유지해 "선택 계좌 = 표시된 데이터" 정합성을 지킨다.
                    selectedAccountSeq = if (portfolioSuccess != null) {
                        targetAccountSeq ?: selectedAccountSeq
                    } else {
                        selectedAccountSeq
                    },
                    portfolio = portfolioSuccess?.data ?: portfolio,
                    errorMessage = firstErrorMessage(accountsResult, portfolioResult),
                )
            }
        }
    }

    /** 계좌목록 팝업에서 다른 계좌 선택 — 계좌목록은 그대로 두고 포트폴리오만 재조회한다. */
    private fun selectAccount(accountSeq: Long) {
        val current = uiState.value
        if (current.selectedAccountSeq == accountSeq || current.isLoading || current.isRefreshing) {
            return
        }
        val previousAccountSeq = current.selectedAccountSeq

        viewModelScope.launch(dispatcherProvider.io) {
            updateState {
                copy(selectedAccountSeq = accountSeq, isLoading = true, errorMessage = null)
            }

            val portfolioResult = fetchPortfolioUseCase(accountSeq)
            val portfolioSuccess = portfolioResult as? NetworkResult.Success

            updateState {
                copy(
                    isLoading = false,
                    // 실패 시 화면에 남는 포트폴리오는 이전 계좌 데이터이므로,
                    // selectedAccountSeq도 이전 계좌로 되돌려 데이터 불일치를 방지한다.
                    selectedAccountSeq = if (portfolioSuccess != null) accountSeq else previousAccountSeq,
                    portfolio = portfolioSuccess?.data ?: portfolio,
                    errorMessage = firstErrorMessage(portfolioResult),
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
