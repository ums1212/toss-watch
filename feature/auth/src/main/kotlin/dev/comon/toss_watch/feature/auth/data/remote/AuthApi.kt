package dev.comon.toss_watch.feature.auth.data.remote

import dev.comon.toss_watch.feature.auth.data.remote.dto.GoogleLoginRequest
import dev.comon.toss_watch.feature.auth.data.remote.dto.GoogleLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    /** 구글 ID Token 검증 + 서비스 JWT 발급 (신규 유저 자동 가입). */
    @POST("v1/toss-watch/auth/google/")
    suspend fun loginWithGoogle(
        @Body body: GoogleLoginRequest,
    ): Response<GoogleLoginResponse>
}
