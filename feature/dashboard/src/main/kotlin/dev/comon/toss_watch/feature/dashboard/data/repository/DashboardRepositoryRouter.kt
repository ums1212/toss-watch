package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository

/**
 * [remote]/[guest] 중 현재 세션이 게스트 모드인지에 따라 호출을 위임하는 라우터.
 *
 * `@Binds`는 컴파일 타임에 구현체가 고정되어 런타임 전환이 불가능하므로,
 * Hilt 모듈에서 이 라우터를 `@Provides`로 조립해 [DashboardRepository]로 주입한다.
 * suspend 호출 시점에만 분기하면 되므로 [GuestModeStore.isGuestMode]의 동기 조회로 충분하다.
 */
internal class DashboardRepositoryRouter(
    private val remote: DashboardRepository,
    private val guest: DashboardRepository,
    private val guestModeStore: GuestModeStore,
) : DashboardRepository {

    private fun current(): DashboardRepository =
        if (guestModeStore.isGuestMode()) guest else remote

    override suspend fun fetchAccounts(): NetworkResult<List<Account>> = current().fetchAccounts()

    override suspend fun fetchPortfolio(accountSeq: Long?): NetworkResult<Portfolio> =
        current().fetchPortfolio(accountSeq)
}
