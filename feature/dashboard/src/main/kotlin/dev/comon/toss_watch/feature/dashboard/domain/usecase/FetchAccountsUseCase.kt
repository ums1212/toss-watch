package dev.comon.toss_watch.feature.dashboard.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

/** 유저가 보유한 토스증권 계좌 목록 조회. */
class FetchAccountsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) {
    suspend operator fun invoke(): NetworkResult<List<Account>> =
        dashboardRepository.fetchAccounts()
}
