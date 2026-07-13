package dev.comon.toss_watch.core.model

/**
 * 모든 API 호출 결과를 Domain 레이어에 전달하기 전에 감싸는 공통 래퍼.
 *
 * - [Success]: 2xx 응답과 파싱된 바디
 * - [ApiError]: 서버가 응답했지만 실패한 경우 (4xx/5xx)
 * - [NetworkError]: 요청 자체가 실패한 경우 (IO, 타임아웃, 파싱 실패 등)
 */
sealed interface NetworkResult<out T> {

    data class Success<T>(val data: T) : NetworkResult<T>

    data class ApiError(
        val code: Int,
        val message: String?,
    ) : NetworkResult<Nothing>

    data class NetworkError(
        val throwable: Throwable,
    ) : NetworkResult<Nothing>
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data))
        is NetworkResult.ApiError -> this
        is NetworkResult.NetworkError -> this
    }

inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (NetworkResult<Nothing>) -> Unit): NetworkResult<T> {
    when (this) {
        is NetworkResult.ApiError -> action(this)
        is NetworkResult.NetworkError -> action(this)
        is NetworkResult.Success -> Unit
    }
    return this
}
