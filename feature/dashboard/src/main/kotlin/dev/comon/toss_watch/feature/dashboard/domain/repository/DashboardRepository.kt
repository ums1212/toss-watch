package dev.comon.toss_watch.feature.dashboard.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets

interface DashboardRepository {

    suspend fun fetchUserAssets(): NetworkResult<UserAssets>

    suspend fun fetchTargetTickers(): NetworkResult<List<TargetTicker>>
}
