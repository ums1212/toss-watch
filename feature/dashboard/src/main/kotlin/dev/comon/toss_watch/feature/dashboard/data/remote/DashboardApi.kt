package dev.comon.toss_watch.feature.dashboard.data.remote

import dev.comon.toss_watch.feature.dashboard.data.remote.dto.TargetTickerResponse
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.UserAssetsResponse
import retrofit2.Response
import retrofit2.http.GET

interface DashboardApi {

    /** 계좌 자산 요약 (Django가 증권사 연동 결과를 집계해 반환). */
    @GET("v1/dashboard/assets/")
    suspend fun getUserAssets(): Response<UserAssetsResponse>

    /** 알림 대상 관찰 종목 + 실시간 시세 스냅샷. */
    @GET("v1/dashboard/tickers/")
    suspend fun getTargetTickers(): Response<List<TargetTickerResponse>>
}
