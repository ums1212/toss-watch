package dev.comon.toss_watch.feature.tosskey.data.remote

import dev.comon.toss_watch.feature.tosskey.data.remote.dto.TossKeyRequest
import dev.comon.toss_watch.feature.tosskey.data.remote.dto.TossKeyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TossKeyApi {

    /** 유저 본인의 토스증권 Open API 키(client_id/client_secret) 등록/수정. */
    @POST("v1/users/toss-key/")
    suspend fun registerTossKey(
        @Body body: TossKeyRequest,
    ): Response<TossKeyResponse>
}
