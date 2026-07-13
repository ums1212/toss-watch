package dev.comon.toss_watch.feature.dashboard.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

/** 계좌 자산 요약 조회. */
class FetchUserAssetsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) {
    suspend operator fun invoke(): NetworkResult<UserAssets> =
        dashboardRepository.fetchUserAssets()
}
