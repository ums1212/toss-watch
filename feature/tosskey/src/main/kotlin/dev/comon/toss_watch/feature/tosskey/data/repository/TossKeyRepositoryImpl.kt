package dev.comon.toss_watch.feature.tosskey.data.repository

import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.toss_watch.core.model.onSuccess
import dev.comon.toss_watch.core.network.safeApiCall
import dev.comon.toss_watch.feature.tosskey.data.remote.TossKeyApi
import dev.comon.toss_watch.feature.tosskey.data.remote.dto.TossKeyRequest
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TossKeyRepositoryImpl @Inject constructor(
    private val tossKeyApi: TossKeyApi,
    private val tokenStore: TokenStore,
) : TossKeyRepository {

    override suspend fun registerTossKey(
        clientId: String,
        clientSecret: String,
    ): NetworkResult<Unit> =
        safeApiCall {
            tossKeyApi.registerTossKey(
                TossKeyRequest(clientId = clientId, clientSecret = clientSecret),
            )
        }
            .onSuccess { tokenStore.setTossKeyRegistered(true) }
            .map { }
}
