package dev.comon.watch_app.domain.model

/** 알람 화면에 표시할 시세. price/changeRate는 서버가 포맷한 문자열 그대로다(예: "72500", "+2.30%"). */
data class StockQuote(
    val stockCode: String,
    val stockName: String,
    val price: String,
    val changeRate: String,
)
