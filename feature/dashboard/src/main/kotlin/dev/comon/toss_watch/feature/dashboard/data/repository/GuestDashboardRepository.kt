package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.data.guest.GuestDashboardData
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 게스트(더미 데이터 체험) 모드용 [DashboardRepository] — 네트워크를 전혀 호출하지 않고
 * [GuestDashboardData]의 샘플 계좌/포트폴리오를 즉시 반환한다.
 *
 * 실 사용자의 [dev.comon.toss_watch.core.database.PortfolioStockCache](Room)를 오염시키지 않도록
 * 의도적으로 건드리지 않는다 — 실 구현([DashboardRepositoryImpl])만 그 캐시를 갱신한다.
 */
@Singleton
internal class GuestDashboardRepository @Inject constructor() : DashboardRepository {

    override suspend fun fetchAccounts(): NetworkResult<List<Account>> =
        NetworkResult.Success(GuestDashboardData.ACCOUNTS)

    override suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio> {
        val portfolio = GuestDashboardData.PORTFOLIOS[accountSeq]
            ?: GuestDashboardData.PORTFOLIOS.getValue(GuestDashboardData.ACCOUNT_MAIN.accountSeq)
        return NetworkResult.Success(portfolio)
    }
}
