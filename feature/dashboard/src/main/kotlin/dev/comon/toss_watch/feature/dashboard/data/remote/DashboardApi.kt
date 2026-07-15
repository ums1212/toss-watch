package dev.comon.toss_watch.feature.dashboard.data.remote

import dev.comon.toss_watch.feature.dashboard.data.remote.dto.AccountResponse
import dev.comon.toss_watch.feature.dashboard.data.remote.dto.PortfolioResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {

    /** 유저가 등록한 토스 API 키로 토스증권에 개설된 계좌 목록을 조회한다. */
    @GET("v1/toss/accounts/")
    suspend fun getAccounts(): Response<List<AccountResponse>>

    /**
     * 계좌 포트폴리오(보유 종목 잔고) 조회.
     *
     * @param accountSeq 특정 계좌의 식별 키. `null`이면 서버가 계좌 목록 중 첫 번째 계좌를 기본값으로 사용한다.
     */
    @GET("v1/toss/portfolio/")
    suspend fun getPortfolio(
        @Query("accountSeq") accountSeq: Long? = null,
    ): Response<PortfolioResponse>
}
