package dev.comon.toss_watch.feature.alarm.domain.usecase

import dev.comon.toss_watch.feature.alarm.domain.model.AlarmAddViolation
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.util.FakeAlarmRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidateAlarmAddUseCaseTest {

    private val fakeRepository = FakeAlarmRepository()
    private val validate = ValidateAlarmAddUseCase(fakeRepository)

    private fun alarm(
        id: Long,
        stockCode: String = "005930",
        hour: Int = 9,
        minute: Int = 0,
        daysOfWeek: List<Int> = listOf(0, 2),
        isEnabled: Boolean = true,
    ) = AlarmProfile(id, stockCode, "삼성전자", hour, minute, daysOfWeek, isEnabled)

    @Test
    fun `겹치는 알림이 없으면 null을 반환한다`() = runTest {
        fakeRepository.alarmProfiles.value = listOf(alarm(1L))

        assertNull(validate("005930", 9, 0, listOf(1, 3)))
    }

    @Test
    fun `같은 종목·시각에 요일이 하나라도 겹치면 DUPLICATE다`() = runTest {
        fakeRepository.alarmProfiles.value = listOf(alarm(1L))

        assertEquals(AlarmAddViolation.DUPLICATE, validate("005930", 9, 0, listOf(4, 0)))
    }

    @Test
    fun `꺼진 알림과 겹쳐도 DUPLICATE다`() = runTest {
        fakeRepository.alarmProfiles.value = listOf(alarm(1L, isEnabled = false))

        assertEquals(AlarmAddViolation.DUPLICATE, validate("005930", 9, 0, listOf(0)))
    }

    @Test
    fun `종목이나 시각이 다르면 요일이 겹쳐도 허용한다`() = runTest {
        fakeRepository.alarmProfiles.value = listOf(alarm(1L))

        assertNull(validate("035420", 9, 0, listOf(0, 2)))
        assertNull(validate("005930", 9, 1, listOf(0, 2)))
        assertNull(validate("005930", 10, 0, listOf(0, 2)))
    }

    @Test
    fun `최대 개수에 도달하면 LIMIT_REACHED다`() = runTest {
        fakeRepository.alarmProfiles.value =
            List(AlarmProfile.MAX_COUNT) { alarm(it + 1L, hour = it / 60, minute = it % 60) }

        assertEquals(AlarmAddViolation.LIMIT_REACHED, validate("035420", 15, 30, listOf(0)))
    }

    @Test
    fun `최대 개수보다 하나 적으면 허용한다`() = runTest {
        fakeRepository.alarmProfiles.value =
            List(AlarmProfile.MAX_COUNT - 1) { alarm(it + 1L, hour = it / 60, minute = it % 60) }

        assertNull(validate("035420", 15, 30, listOf(0)))
    }
}
