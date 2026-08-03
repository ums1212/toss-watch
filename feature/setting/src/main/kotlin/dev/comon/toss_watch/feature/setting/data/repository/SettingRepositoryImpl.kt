package dev.comon.toss_watch.feature.setting.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.setting.data.remote.SettingApi
import dev.comon.toss_watch.feature.setting.data.remote.dto.WatchTokenRequest
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingRepositoryImpl @Inject constructor(
    private val settingApi: SettingApi,
    private val tokenStore: TokenStore,
    private val guestModeStore: GuestModeStore,
) : SettingRepository {

    override suspend fun registerWatchToken(
        fcmToken: String,
        uuid: String,
        modelName: String,
    ): NetworkResult<Unit> =
        safeApiCall {
            settingApi.registerWatchToken(
                WatchTokenRequest(fcmToken = fcmToken, uuid = uuid, modelName = modelName),
            )
        }
            .onSuccess { response ->
                // 서버가 실제로 저장한 값(응답 body)을 반영한다 — 빈 모델명은 서버가 NULL로
                // 정규화하므로 요청값이 아니라 응답값이 화면과 일치해야 한다. 응답 uuid가
                // 비어 있는 예외 상황에서만 요청 uuid로 폴백해 연동 상태 유실을 막는다.
                persistPairedWatch(
                    hasFcmToken = true,
                    modelName = response.modelName,
                    uuid = response.uuid?.takeIf { it.isNotBlank() } ?: uuid,
                    linkedAt = response.linkedAt,
                )
            }
            .map { }

    override fun observePairedWatch(): Flow<PairedWatchInfo?> = tokenStore.observePairedWatch()

    override suspend fun syncPairedWatch(): NetworkResult<Unit> =
        safeApiCall { settingApi.getWatchTokenStatus() }
            .onSuccess { status ->
                persistPairedWatch(
                    hasFcmToken = status.hasFcmToken,
                    modelName = status.modelName,
                    uuid = status.uuid,
                    linkedAt = status.linkedAt,
                )
            }
            .map { }

    /** PUT/GET 응답의 연동 정보를 로컬(core:datastore)에 반영한다. 미등록이면 정리한다. */
    private fun persistPairedWatch(hasFcmToken: Boolean, modelName: String?, uuid: String?, linkedAt: String?) {
        if (hasFcmToken && !uuid.isNullOrBlank()) {
            tokenStore.setPairedWatch(
                modelName = modelName,
                uuid = uuid,
                linkedAt = linkedAt.toLinkedAtInstant(),
            )
        } else {
            tokenStore.clearPairedWatch()
        }
    }

    /** 서버 ISO 8601 문자열("2026-08-03T22:30:00.123456+09:00") → Instant. 파싱 실패 시 null. */
    private fun String?.toLinkedAtInstant(): Instant? =
        this?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

    override fun logout() {
        tokenStore.clear()
        // 게스트로 진입했을 수도 있으니 잔여 플래그를 함께 정리한다 — 실 로그아웃이었다면 이미
        // false 상태라 no-op이다.
        guestModeStore.exitGuestMode()
    }

    override fun observeGuestMode() = guestModeStore.observeGuestMode()
}
