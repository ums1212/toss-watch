package dev.comon.toss_watch.feature.alarm.data.repository

import dev.comon.toss_watch.feature.alarm.util.FakeAlarmRepository
import dev.comon.toss_watch.feature.alarm.util.FakeGuestModeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmRepositoryRouterTest {

    private val remote = FakeAlarmRepository()
    private val guest = FakeAlarmRepository()
    private val guestModeStore = FakeGuestModeStore()
    private val router = AlarmRepositoryRouter(remote, guest, guestModeStore)

    @Test
    fun `게스트 모드가 아니면 remote 알림 스트림을 구독한다`() =
        runTest {
            remote.alarmProfiles.value = FakeAlarmRepository.DEFAULT_ALARMS

            val emitted = router.observeAlarmProfiles().first()

            assertEquals(FakeAlarmRepository.DEFAULT_ALARMS, emitted)
        }

    @Test
    fun `게스트 모드로 전환하면 guest 알림 스트림으로 즉시 갈아탄다`() =
        runTest {
            remote.alarmProfiles.value = FakeAlarmRepository.DEFAULT_ALARMS
            guest.alarmProfiles.value = listOf(FakeAlarmRepository.ADDED_ALARM)

            guestModeStore.enterGuestMode()
            val emitted = router.observeAlarmProfiles().first()

            assertEquals(listOf(FakeAlarmRepository.ADDED_ALARM), emitted)
        }

    @Test
    fun `게스트 모드에서 알림 추가는 guest 리포지토리에만 반영되고 remote는 건드리지 않는다`() =
        runTest {
            guestModeStore.enterGuestMode()

            router.addAlarmProfile(
                stockCode = "005930",
                stockName = "삼성전자",
                hour = 9,
                minute = 0,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
            )

            assertEquals("005930", guest.lastAddedStockCode)
            assertNull(remote.lastAddedStockCode)
        }

    @Test
    fun `게스트 모드가 아니면 알림 추가는 remote 리포지토리에만 반영된다`() =
        runTest {
            router.addAlarmProfile(
                stockCode = "005930",
                stockName = "삼성전자",
                hour = 9,
                minute = 0,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
            )

            assertEquals("005930", remote.lastAddedStockCode)
            assertNull(guest.lastAddedStockCode)
        }

    @Test
    fun `게스트 모드에서 보유 종목 스트림도 guest 캐시로 전환된다`() =
        runTest {
            guest.portfolioStocks.value = FakeAlarmRepository.DEFAULT_STOCKS

            guestModeStore.enterGuestMode()
            val emitted = router.observePortfolioStocks().first()

            assertEquals(FakeAlarmRepository.DEFAULT_STOCKS, emitted)
        }
}
