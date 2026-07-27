package dev.comon.toss_watch.feature.alarm.domain.repository

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {

    /**
     * 등록된 알림 프로필 전체의 반응형 스트림 — 인메모리 캐시를 관측한다.
     * AlarmScreen(종목별 요약)과 AlarmDetailScreen(종목별 상세)이 같은 스트림을 구독하므로,
     * 한쪽에서 추가/토글/삭제하면 다른 화면도 재조회 없이 즉시 최신 상태를 반영한다.
     */
    fun observeAlarmProfiles(): Flow<List<AlarmProfile>>

    /** 서버에서 알림 프로필 목록을 다시 조회해 캐시를 갈아끼운다. */
    suspend fun refreshAlarmProfiles(): NetworkResult<Unit>

    /**
     * 대시보드가 캐싱해 둔 "현재 표시 중인 포트폴리오" 보유 종목을 구독한다.
     * API를 재호출하지 않고 [dev.comon.toss_watch.core.database.PortfolioStockCache]를 그대로 위임한다.
     */
    fun observePortfolioStocks(): Flow<List<CachedStock>>

    suspend fun addAlarmProfile(
        stockCode: String,
        stockName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>,
    ): NetworkResult<AlarmProfile>

    suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile>

    /** 알림 프로필 삭제 (DELETE /api/v1/toss-watch/notifications/{id}/). */
    suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit>

    /**
     * 캐시에만 활성 여부를 즉시 반영한다 — 서버 응답을 기다리지 않는 낙관적 업데이트,
     * 그리고 실패 시 이전 값으로 되돌리는 롤백 양쪽에 쓰인다.
     */
    fun setAlarmEnabledInCache(alarmId: Long, isEnabled: Boolean)
}
