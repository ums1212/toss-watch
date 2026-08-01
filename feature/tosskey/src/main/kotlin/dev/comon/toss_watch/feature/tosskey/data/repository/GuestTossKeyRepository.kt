package dev.comon.toss_watch.feature.tosskey.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 게스트(더미 데이터 체험) 모드용 [TossKeyRepository] — 실 API 호출과 [dev.comon.toss_watch.core.datastore.TokenStore]
 * 쓰기 없이 항상 성공으로 응답한다. 화면은 그대로 유지해 심사자가 등록 플로우를 체험할 수 있게 하되,
 * 저장 결과는 더미로 처리한다.
 */
@Singleton
internal class GuestTossKeyRepository @Inject constructor() : TossKeyRepository {

    override suspend fun registerTossKey(clientId: String, clientSecret: String): NetworkResult<Unit> =
        NetworkResult.Success(Unit)
}
