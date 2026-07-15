package dev.comon.toss_watch.feature.dashboard.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

/**
 * 계좌 포트폴리오(보유 종목 잔고) 조회.
 *
 * @param accountSeq 조회할 계좌의 식별 키. `null`이면 서버가 첫 번째 계좌를 기본값으로 사용한다.
 */
class FetchPortfolioUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) {
    suspend operator fun invoke(accountSeq: Long?): NetworkResult<Portfolio> =
        dashboardRepository.fetchPortfolio(accountSeq)
}
