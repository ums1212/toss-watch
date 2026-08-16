package dev.comon.toss_watch.feature.dashboard.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [PortfolioResponse] 역직렬화를 검증한다. 특히 `exchange_rate`는 서버가 붙여준 지 얼마 안 된
 * 필드라 응답에서 누락될 가능성을 방어적으로 처리해야 한다.
 */
class PortfolioResponseTest {

    @Test
    fun `exchange_rate 필드가 있으면 파싱해 toPortfolio에 그대로 전달한다`() {
        val json = """
            {
              "summary": {
                "total_investment_krw": 650000,
                "total_investment_usd": 2600000,
                "total_evaluation_krw": 725000,
                "total_evaluation_usd": 3400000,
                "total_profit_loss_krw": 75000,
                "total_profit_loss_usd": 800000,
                "total_return_rate": "26.92"
              },
              "securities": [],
              "exchange_rate": 1380.5
            }
        """.trimIndent()

        val response = Json.decodeFromString<PortfolioResponse>(json)

        assertEquals(1380.5, response.exchangeRate, DELTA)
        assertEquals(1380.5, response.toPortfolio().exchangeRate, DELTA)
    }

    @Test
    fun `exchange_rate 필드가 누락되면 0으로 기본 처리한다`() {
        val json = """
            {
              "summary": {
                "total_investment_krw": 650000,
                "total_investment_usd": 0,
                "total_evaluation_krw": 725000,
                "total_evaluation_usd": 0,
                "total_profit_loss_krw": 75000,
                "total_profit_loss_usd": 0,
                "total_return_rate": "11.54"
              },
              "securities": []
            }
        """.trimIndent()

        val response = Json.decodeFromString<PortfolioResponse>(json)

        assertEquals(0.0, response.exchangeRate, DELTA)
        assertEquals(0.0, response.toPortfolio().exchangeRate, DELTA)
    }

    companion object {
        private const val DELTA = 0.001
    }
}
