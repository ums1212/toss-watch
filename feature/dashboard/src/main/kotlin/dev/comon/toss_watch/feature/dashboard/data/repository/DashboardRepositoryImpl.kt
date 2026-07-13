package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.dashboard.data.remote.DashboardApi
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toTargetTicker
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.toUserAssets
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi,
) : DashboardRepository {

    override suspend fun fetchUserAssets(): NetworkResult<UserAssets> =
        safeApiCall { dashboardApi.getUserAssets() }
            .map { it.toUserAssets() }

    override suspend fun fetchTargetTickers(): NetworkResult<List<TargetTicker>> =
        safeApiCall { dashboardApi.getTargetTickers() }
            .map { list -> list.map { it.toTargetTicker() } }
}
