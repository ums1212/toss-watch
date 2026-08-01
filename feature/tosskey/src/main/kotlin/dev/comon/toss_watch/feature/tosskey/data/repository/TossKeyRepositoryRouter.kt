package dev.comon.toss_watch.feature.tosskey.data.repository

import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository

/**
 * [remote]/[guest] 중 현재 세션이 게스트 모드인지에 따라 호출을 위임하는 라우터.
 *
 * `@Binds`는 컴파일 타임에 구현체가 고정되어 런타임 전환이 불가능하므로,
 * Hilt 모듈에서 이 라우터를 `@Provides`로 조립해 [TossKeyRepository]로 주입한다.
 */
internal class TossKeyRepositoryRouter(
    private val remote: TossKeyRepository,
    private val guest: TossKeyRepository,
    private val guestModeStore: GuestModeStore,
) : TossKeyRepository {

    override suspend fun registerTossKey(clientId: String, clientSecret: String): NetworkResult<Unit> =
        if (guestModeStore.isGuestMode()) {
            guest.registerTossKey(clientId, clientSecret)
        } else {
            remote.registerTossKey(clientId, clientSecret)
        }
}
