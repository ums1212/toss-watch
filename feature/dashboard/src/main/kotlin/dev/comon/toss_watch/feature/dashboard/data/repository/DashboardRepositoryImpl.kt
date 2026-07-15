package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.dashboard.data.remote.DashboardApi
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toAccount
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toPortfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi,
) : DashboardRepository {

    override suspend fun fetchAccounts(): NetworkResult<List<Account>> =
        safeApiCall { dashboardApi.getAccounts() }
            .map { list -> list.map { it.toAccount() } }

    override suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio> =
        safeApiCall { dashboardApi.getPortfolio(accountSeq) }
            .map { it.toPortfolio() }
}
