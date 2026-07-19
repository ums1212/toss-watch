package dev.comon.toss_watch.feature.setting.domain.usecase

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 대시보드가 캐싱해 둔 보유 종목 목록을 구독 — 알림 추가 다이얼로그의 종목 후보로 쓰인다. */
class ObservePortfolioStocksUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
) {
    operator fun invoke(): Flow<List<CachedStock>> = settingRepository.observePortfolioStocks()
}
