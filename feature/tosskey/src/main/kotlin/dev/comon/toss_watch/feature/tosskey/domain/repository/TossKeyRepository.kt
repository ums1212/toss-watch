package dev.comon.toss_watch.feature.tosskey.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult

interface TossKeyRepository {

    suspend fun registerTossKey(clientId: String, clientSecret: String): NetworkResult<Unit>
}
