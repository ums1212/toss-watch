package dev.comon.toss_watch.feature.alarm.data.guest

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile

/**
 * 게스트(더미 데이터 체험) 모드에서 알림 화면에 채워 넣는 샘플 데이터.
 *
 * `:feature:alarm`은 `:feature:dashboard`에 의존하지 않으므로(CLAUDE.md §2 — feature 모듈 간
 * 직접 의존 금지) 종목 코드/종목명은 `GuestDashboardData`의 값과 문자열로만 맞춰 둔다 — 실제
 * import 의존은 없다.
 */
object GuestAlarmData {

    /** 대시보드 게스트 포트폴리오의 종목과 코드/종목명을 맞춘 후보 목록. */
    val PORTFOLIO_STOCKS = listOf(
        CachedStock(stockCode = "005930", stockName = "삼성전자"),
        CachedStock(stockCode = "000660", stockName = "SK하이닉스"),
        CachedStock(stockCode = "035420", stockName = "NAVER"),
    )

    /**
     * 게스트 세션 진입 시 채워 넣는 초기 알림 3건 — 2개 종목에 분산해
     * [dev.comon.toss_watch.feature.alarm.presentation.alarm.AlarmScreen]의 종목별 개수 표시가
     * 의미 있게 보이도록 한다.
     */
    val SEED_ALARMS = listOf(
        AlarmProfile(
            id = 9_001L,
            stockCode = "005930",
            stockName = "삼성전자",
            hour = 9,
            minute = 0,
            daysOfWeek = listOf(0, 1, 2, 3, 4),
            isEnabled = true,
        ),
        AlarmProfile(
            id = 9_002L,
            stockCode = "005930",
            stockName = "삼성전자",
            hour = 15,
            minute = 20,
            daysOfWeek = listOf(0, 1, 2, 3, 4),
            isEnabled = false,
        ),
        AlarmProfile(
            id = 9_003L,
            stockCode = "000660",
            stockName = "SK하이닉스",
            hour = 9,
            minute = 5,
            daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6),
            isEnabled = true,
        ),
    )

    /** 시드 알림과 겹치지 않도록 신규 게스트 알림 ID는 이 값보다 크게 발급한다. */
    const val SEED_ID_UPPER_BOUND = 9_100L
}
