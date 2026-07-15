package dev.comon.toss_watch.feature.dashboard.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio

interface DashboardRepository {

    suspend fun fetchAccounts(): NetworkResult<List<Account>>

    suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio>
}
