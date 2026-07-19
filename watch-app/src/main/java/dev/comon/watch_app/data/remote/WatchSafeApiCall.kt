package dev.comon.watch_app.data.remote

import dev.comon.toss_watch.core.model.NetworkResult
import retrofit2.HttpException
import retrofit2.Response

/**
 * `:core:network`의 `safeApiCall`과 동일한 역할을 하는 워치앱 전용 최소 구현.
 *
 * `:core:network`는 JWT 세션/Tink 암호화 등 폰 로그인 전용 인프라(AuthInterceptor,
 * TokenAuthenticator, core:datastore)를 함께 끌고 오므로, 로그인 세션이 없는
 * 워치앱(2-5 API는 JWT 불필요)에는 이 경량 버전만 둔다.
 */
suspend fun <T> watchSafeApiCall(execute: suspend () -> Response<T>): NetworkResult<T> =
    try {
        val response = execute()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            NetworkResult.Success(body)
        } else {
            NetworkResult.ApiError(
                code = response.code(),
                message = response.errorBody()?.string() ?: response.message(),
            )
        }
    } catch (e: HttpException) {
        NetworkResult.ApiError(code = e.code(), message = e.message())
    } catch (e: Exception) {
        NetworkResult.NetworkError(e)
    }
