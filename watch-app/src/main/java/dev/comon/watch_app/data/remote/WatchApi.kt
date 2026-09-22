package dev.comon.watch_app.data.remote

import dev.comon.watch_app.data.remote.dto.FcmTokenCheckRequest
import dev.comon.watch_app.data.remote.dto.FcmTokenCheckResponse
import dev.comon.watch_app.data.remote.dto.StockQuoteRequest
import dev.comon.watch_app.data.remote.dto.StockQuoteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface WatchApi {

    /**
     * 2-5. 워치 FCM 토큰 등록 여부 확인 (워치앱 전용, JWT 불필요).
     * 로그인 세션 없이 로컬에 발급받은 FCM 토큰이 서버 기준으로 등록됐는지만 확인한다.
     * `X-Toss-Watch-Api-Key` 헤더는 [dev.comon.watch_app.di.WatchApiKeyInterceptor]가 부착한다.
     */
    @POST("v1/toss-watch/fcm-token/check/")
    suspend fun checkFcmToken(
        @Body body: FcmTokenCheckRequest,
    ): Response<FcmTokenCheckResponse>

    /**
     * 2-6. 워치 알람 시세 조회 (워치앱 전용, JWT 불필요).
     * 로컬 알람이 울리는 즉시 호출해, 사용자가 알람 화면을 누르기 전에 시세를 미리 받아둔다.
     * 서버는 [StockQuoteRequest.uuid]로 연동된 유저를 찾아 그 유저의 토스 키로 조회한다.
     */
    @POST("v1/toss-watch/watch/stock-quote/")
    suspend fun getStockQuote(
        @Body body: StockQuoteRequest,
    ): Response<StockQuoteResponse>
}
