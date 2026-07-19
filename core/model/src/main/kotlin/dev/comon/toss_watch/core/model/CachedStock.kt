package dev.comon.toss_watch.core.model

/**
 * 대시보드 포트폴리오 조회 시점에 로컬(RoomDB)에 캐싱되는 보유 종목 스냅샷.
 *
 * `feature:dashboard`가 포트폴리오를 조회할 때마다 이 모델로 캐시를 갈아끼우고,
 * `feature:setting`은 API를 재호출하지 않고 이 캐시를 구독해 알림 추가 종목 후보로 쓴다.
 * 두 feature 모듈이 서로 의존하지 않도록 공유 지점을 core:model에 둔다.
 */
data class CachedStock(
    val stockCode: String,
    val stockName: String,
)
