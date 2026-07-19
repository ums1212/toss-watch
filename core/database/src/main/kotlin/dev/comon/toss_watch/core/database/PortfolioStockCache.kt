package dev.comon.toss_watch.core.database

import dev.comon.toss_watch.core.model.CachedStock
import kotlinx.coroutines.flow.Flow

/**
 * 대시보드가 마지막으로 조회한 포트폴리오의 보유 종목 캐시.
 *
 * `feature:dashboard`는 포트폴리오 조회에 성공할 때마다 [replaceStocks]로 캐시를 갈아끼우고,
 * `feature:setting`은 API를 다시 호출하지 않고 [observeStocks]를 구독해 알림 추가 화면의
 * 종목 후보로 사용한다. 두 feature 모듈이 서로 의존하지 않도록 core:database가 공유 지점 역할을 한다.
 */
interface PortfolioStockCache {

    fun observeStocks(): Flow<List<CachedStock>>

    suspend fun replaceStocks(stocks: List<CachedStock>)
}
