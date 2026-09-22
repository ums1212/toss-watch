package dev.comon.watch_app.data.remote.dto

import dev.comon.watch_app.domain.model.StockQuote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 2-6. 워치 알람 시세 조회 (`POST /watch/stock-quote/`) 요청 바디. */
@Serializable
data class StockQuoteRequest(
    @SerialName("uuid") val uuid: String,
    @SerialName("stock_code") val stockCode: String,
)

/** 2-6. 워치 알람 시세 조회 응답 바디. 값 포맷은 기존 FCM 알림(§5) 규격과 같다. */
@Serializable
data class StockQuoteResponse(
    @SerialName("stock_code") val stockCode: String,
    @SerialName("stock_name") val stockName: String = "",
    @SerialName("price") val price: String,
    @SerialName("change_rate") val changeRate: String = "",
    @SerialName("timestamp") val timestamp: String = "",
)

fun StockQuoteResponse.toDomain() = StockQuote(
    stockCode = stockCode,
    stockName = stockName,
    price = price,
    changeRate = changeRate,
)
