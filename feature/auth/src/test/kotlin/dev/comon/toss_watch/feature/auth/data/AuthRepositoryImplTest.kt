package dev.comon.toss_watch.feature.auth.data

import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.data.remote.AuthApi
import dev.comon.toss_watch.feature.auth.data.remote.dto.GoogleLoginRequest
import dev.comon.toss_watch.feature.auth.data.remote.dto.GoogleLoginResponse
import dev.comon.toss_watch.feature.auth.data.repository.AuthRepositoryImpl
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private val fakeTokenStore = FakeTokenStore()

    @Test
    fun `로그인 성공 시 UserSession으로 매핑되고 토큰이 저장소에 커밋된다`() = runTest {
        val repository = AuthRepositoryImpl(
            authApi = FakeAuthApi { Response.success(SUCCESS_RESPONSE) },
            tokenStore = fakeTokenStore,
        )

        val result = repository.loginWithGoogle("id-token")

        val session = (result as NetworkResult.Success).data
        assertEquals("user@gmail.com", session.email)
        assertTrue(session.isNewUser)
        assertEquals(false, session.hasTossKey)

        assertEquals("access-jwt", fakeTokenStore.getAccessToken())
        assertEquals("refresh-jwt", fakeTokenStore.getRefreshToken())
    }

    @Test
    fun `서버 에러 응답이면 ApiError를 반환하고 토큰은 저장하지 않는다`() = runTest {
        val repository = AuthRepositoryImpl(
            authApi = FakeAuthApi {
                Response.error(
                    401,
                    """{"detail":"invalid token"}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            },
            tokenStore = fakeTokenStore,
        )

        val result = repository.loginWithGoogle("id-token")

        assertEquals(401, (result as NetworkResult.ApiError).code)
        assertNull(fakeTokenStore.getAccessToken())
        assertNull(fakeTokenStore.getRefreshToken())
    }

    @Test
    fun `요청 자체가 실패하면 NetworkError를 반환하고 토큰은 저장하지 않는다`() = runTest {
        val repository = AuthRepositoryImpl(
            authApi = FakeAuthApi { throw IOException("no route to host") },
            tokenStore = fakeTokenStore,
        )

        val result = repository.loginWithGoogle("id-token")

        assertTrue(result is NetworkResult.NetworkError)
        assertNull(fakeTokenStore.getAccessToken())
    }

    private class FakeAuthApi(
        private val answer: () -> Response<GoogleLoginResponse>,
    ) : AuthApi {
        override suspend fun loginWithGoogle(
            body: GoogleLoginRequest,
        ): Response<GoogleLoginResponse> = answer()
    }

    private class FakeTokenStore : TokenStore {
        private var accessToken: String? = null
        private var refreshToken: String? = null

        override fun observeHasSession(): kotlinx.coroutines.flow.Flow<Boolean> =
            kotlinx.coroutines.flow.flowOf(refreshToken != null)

        override fun getAccessToken(): String? = accessToken

        override fun getRefreshToken(): String? = refreshToken

        override fun saveTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override fun updateAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
            refreshToken = null
        }
    }

    companion object {
        private val SUCCESS_RESPONSE = GoogleLoginResponse(
            accessToken = "access-jwt",
            refreshToken = "refresh-jwt",
            tokenType = "Bearer",
            isNewUser = true,
            email = "user@gmail.com",
            hasTossKey = false,
        )
    }
}
