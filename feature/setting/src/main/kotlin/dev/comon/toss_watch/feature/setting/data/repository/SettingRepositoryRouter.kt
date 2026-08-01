package dev.comon.toss_watch.feature.setting.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * [remote]/[guest] 중 현재 세션이 게스트 모드인지에 따라 호출을 위임하는 라우터.
 *
 * `@Binds`는 컴파일 타임에 구현체가 고정되어 런타임 전환이 불가능하므로,
 * Hilt 모듈에서 이 라우터를 `@Provides`로 조립해 [SettingRepository]로 주입한다.
 *
 * [logout]과 [observeGuestMode]는 게스트 여부로 분기하지 않고 항상 [remote]로 위임한다 —
 * 게스트 이탈이든 실 로그아웃이든 "세션 토큰 정리 + 게스트 플래그 정리"라는 동일한 결과가
 * 나와야 하고, 그 로직은 [remote]([dev.comon.toss_watch.feature.setting.data.repository.SettingRepositoryImpl])에만 있다.
 */
internal class SettingRepositoryRouter(
    private val remote: SettingRepository,
    private val guest: SettingRepository,
    private val guestModeStore: GuestModeStore,
) : SettingRepository {

    private fun current(): SettingRepository =
        if (guestModeStore.isGuestMode()) guest else remote

    override suspend fun registerWatchToken(
        fcmToken: String,
        uuid: String,
        modelName: String,
    ): NetworkResult<Unit> = current().registerWatchToken(fcmToken, uuid, modelName)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePairedWatch(): Flow<PairedWatchInfo?> =
        guestModeStore.observeGuestMode().flatMapLatest { isGuest ->
            if (isGuest) guest.observePairedWatch() else remote.observePairedWatch()
        }

    override suspend fun syncPairedWatch(): NetworkResult<Unit> = current().syncPairedWatch()

    override fun logout() = remote.logout()

    override fun observeGuestMode(): Flow<Boolean> = remote.observeGuestMode()
}
