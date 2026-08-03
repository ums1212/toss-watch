package dev.comon.toss_watch.feature.setting.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 게스트(더미 데이터 체험) 모드용 [SettingRepository] — 실 API 호출 없이 인메모리 상태로 동작한다.
 *
 * [pairedWatch]는 초기값 `null`로 두어 설정 화면에 "QR로 워치 연동" 버튼이 노출되게 하고,
 * 등록을 시도하면 화면이 그대로 성공 흐름을 탈 수 있도록 인메모리에 반영한다.
 */
@Singleton
internal class GuestSettingRepository @Inject constructor(
    private val guestModeStore: GuestModeStore,
) : SettingRepository {

    private val pairedWatch = MutableStateFlow<PairedWatchInfo?>(null)

    override suspend fun registerWatchToken(
        fcmToken: String,
        uuid: String,
        modelName: String,
    ): NetworkResult<Unit> {
        pairedWatch.value = PairedWatchInfo(modelName = modelName, uuid = uuid, linkedAt = Instant.now())
        return NetworkResult.Success(Unit)
    }

    override fun observePairedWatch(): Flow<PairedWatchInfo?> = pairedWatch.asStateFlow()

    override suspend fun syncPairedWatch(): NetworkResult<Unit> = NetworkResult.Success(Unit)

    // SettingRepositoryRouter가 로그아웃/게스트 이탈은 항상 remote(SettingRepositoryImpl)로
    // 위임하므로 이 구현은 실제로 호출되지 않는다. 그래도 인터페이스 계약을 지키기 위해
    // 게스트 플래그 정리만 최소한으로 수행한다.
    override fun logout() {
        guestModeStore.exitGuestMode()
    }

    override fun observeGuestMode(): Flow<Boolean> = guestModeStore.observeGuestMode()
}
