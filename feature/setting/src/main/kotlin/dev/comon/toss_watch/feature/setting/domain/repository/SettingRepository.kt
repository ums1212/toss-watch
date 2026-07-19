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

    suspend fun registerWatchToken(fcmToken: String, uuid: String, modelName: String): NetworkResult<Unit>

    /**
     * 연동 완료된 워치(기기명+UUID)의 반응형 스트림.
     * 서버 `GET /users/fcm-token/`는 등록 여부(Boolean)만 반환하므로, 등록 성공(200) 시점에
     * 로컬(core:datastore)에 저장해 둔 값을 그대로 관측한다. 미연동이면 `null`.
     */
    fun observePairedWatch(): Flow<PairedWatchInfo?>
}
