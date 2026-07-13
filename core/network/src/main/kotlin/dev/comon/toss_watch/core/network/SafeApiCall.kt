package dev.comon.toss_watch.core.network

import dev.comon.toss_watch.core.model.NetworkResult
import retrofit2.HttpException
import retrofit2.Response

/**
 * Retrofit suspend 호출을 [NetworkResult]로 변환하는 공통 헬퍼.
 * 각 feature의 RemoteDataSource는 반드시 이 함수를 통해 API를 호출해
 * Domain 레이어에 예외가 아닌 [NetworkResult]만 전달되도록 한다.
 */
suspend fun <T> safeApiCall(execute: suspend () -> Response<T>): NetworkResult<T> =
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
