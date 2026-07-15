package dev.comon.toss_watch.core.network

import dev.comon.toss_watch.core.model.NetworkResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException
import retrofit2.Response

private val errorBodyJson = Json { ignoreUnknownKeys = true }

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
                message = response.errorBody()?.string()?.toUserMessage() ?: response.message(),
            )
        }
    } catch (e: HttpException) {
        NetworkResult.ApiError(code = e.code(), message = e.message())
    } catch (e: Exception) {
        NetworkResult.NetworkError(e)
    }

/**
 * 서버 에러 응답 바디(JSON)에서 사람이 읽을 수 있는 메시지만 뽑아낸다.
 * `error`/`detail` 키의 문자열 값을 우선 사용하고, 알림 생성 폼 검증처럼
 * `{"field": ["메시지"]}` 형태이거나 JSON이 아닌 응답(예: 게이트웨이 HTML 에러 페이지)이면
 * 원본 문자열을 그대로 반환한다.
 */
private fun String.toUserMessage(): String {
    val jsonObject = runCatching { errorBodyJson.parseToJsonElement(this) }
        .getOrNull() as? JsonObject
        ?: return this

    val message = (jsonObject["error"] as? JsonPrimitive)?.contentOrNull
        ?: (jsonObject["detail"] as? JsonPrimitive)?.contentOrNull

    return message ?: this
}
