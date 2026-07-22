package dev.comon.watch_app.presentation.alarm

import androidx.annotation.DrawableRes
import dev.comon.watch_app.R

/**
 * 등락률(changeRate) 문자열로부터 판정한 시세 방향.
 * 알람 이미지 Scene에서 보여줄 이미지와, 정보 Scene의 배지 색상 판정에 공통으로 쓰인다.
 */
enum class PriceDirection(@param:DrawableRes val imageRes: Int) {
    UP(R.drawable.stock_alarm_up),
    DOWN(R.drawable.stock_alarm_down),
    FLAT(R.drawable.stock_alarm_flat),
}

/**
 * 서버가 내려주는 changeRate 문자열(예: "+1.2%", "-0.8%", "0.00%")을 [PriceDirection]으로 변환한다.
 * 부호는 접두사(`-`)로, 보합은 숫자값이 0인지로 판정한다 — 단순 문자열 비교("0%")보다
 * "0.00%", "+0.0%" 같은 변형까지 안정적으로 보합 처리하기 위함.
 */
fun String.toPriceDirection(): PriceDirection {
    val trimmed = trim()
    if (trimmed.isBlank()) return PriceDirection.FLAT

    val isNegative = trimmed.startsWith("-")
    val numericValue = trimmed
        .trimStart('+', '-')
        .trimEnd('%')
        .toDoubleOrNull()

    return when {
        numericValue == null -> PriceDirection.FLAT
        numericValue == 0.0 -> PriceDirection.FLAT
        isNegative -> PriceDirection.DOWN
        else -> PriceDirection.UP
    }
}
