package dev.comon.toss_watch.feature.setting.domain.repository

import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import kotlinx.coroutines.flow.Flow

interface SettingRepository {

    suspend fun fetchAlarmProfiles(): NetworkResult<List<AlarmProfile>>

    /**
     * 대시보드가 캐싱해 둔 "현재 표시 중인 포트폴리오" 보유 종목을 구독한다.
     * API를 재호출하지 않고 [dev.comon.toss_watch.core.database.PortfolioStockCache]를 그대로 위임한다.
     */
    fun observePortfolioStocks(): Flow<List<CachedStock>>

    suspend fun addAlarmProfile(
        stockCode: String,
        hour: Int,
        minute: Int,
    ): NetworkResult<AlarmProfile>

    suspend fun updateAlarmProfile(
        alarmId: Long,
        isEnabled: Boolean,
    ): NetworkResult<AlarmProfile>

    /** 알림 프로필 삭제 (DELETE /api/v1/toss-watch/notifications/{id}/). */
    suspend fun deleteAlarmProfile(alarmId: Long): NetworkResult<Unit>

    suspend fun registerWatchToken(fcmToken: String, uuid: String, modelName: String): NetworkResult<Unit>

    /**
     * 연동 완료된 워치(기기명+UUID)의 반응형 스트림.
     * QR 등록 성공(200) 시점 또는 [syncPairedWatch] 복원으로 로컬(core:datastore)에
     * 저장해 둔 값을 그대로 관측한다. 미연동이면 `null`.
     */
    fun observePairedWatch(): Flow<PairedWatchInfo?>

    /**
     * 서버 `GET /users/fcm-token/`로 등록 상태를 조회해 로컬(core:datastore) pairedWatch를
     * 서버 기준으로 재동기화한다. 폰앱 재설치 등으로 로컬 값이 유실된 경우 복원하고,
     * 서버가 미등록을 반환하면 로컬 stale 값을 정리한다. best-effort 호출을 전제로 한다.
     */
    suspend fun syncPairedWatch(): NetworkResult<Unit>

    /**
     * 로컬(core:datastore)에 저장된 세션 토큰(Access/Refresh JWT) 및 연동 상태를 모두 제거한다.
     * 서버 측 세션 무효화 API는 없음 — 클라이언트가 토큰을 지우는 즉시
     * :app 최상위 라우터가 `observeHasSession()`을 통해 로그인 화면으로 전환한다.
     */
    fun logout()
}
