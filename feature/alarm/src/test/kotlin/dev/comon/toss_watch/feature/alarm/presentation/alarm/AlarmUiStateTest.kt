package dev.comon.toss_watch.feature.alarm.presentation.alarm

import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmUiStateTest {

    private fun state(vararg counts: Int) = AlarmUiState(
        stockAlarms = counts.mapIndexed { index, count ->
            StockAlarmSummary(stockCode = "00000$index", stockName = "종목$index", alarmCount = count)
        },
    )

    @Test
    fun `종목별 알림 개수의 합이 상한에 도달하면 isAlarmLimitReached다`() {
        assertTrue(state(AlarmProfile.MAX_COUNT - 40, 40).isAlarmLimitReached)
    }

    @Test
    fun `합이 상한보다 하나 적으면 isAlarmLimitReached가 아니다`() {
        assertFalse(state(AlarmProfile.MAX_COUNT - 40, 39).isAlarmLimitReached)
    }
}
