package dev.comon.toss_watch.feature.dashboard.domain.usecase

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

/** 알림 대상 관찰 종목 목록 조회. */
class FetchTargetTickersUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) {
    suspend operator fun invoke(): NetworkResult<List<TargetTicker>> =
        dashboardRepository.fetchTargetTickers()
}
