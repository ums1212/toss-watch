package dev.comon.toss_watch.feature.alarm.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.data.guest.GuestAlarmData
import dev.comon.toss_watch.feature.alarm.util.FakeGuestModeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GuestAlarmRepository]가 세션 기반 재시딩 규칙을 지키는지 검증한다 —
 * `AlarmViewModel`과 `AlarmDetailViewModel`이 둘 다 `init`에서 `refreshAlarmProfiles()`를
 * 호출하므로, 같은 게스트 세션 안에서 재조회해도 사용자가 추가/변경한 알림이 사라지면 안 된다.
 */
class GuestAlarmRepositoryTest {

    private val guestModeStore = FakeGuestModeStore()
    private val repository = GuestAlarmRepository(guestModeStore)

    @Test
    fun `첫 refresh는 시드 알림으로 채운다`() =
        runTest {
            guestModeStore.enterGuestMode()

            repository.refreshAlarmProfiles()

            assertEquals(GuestAlarmData.SEED_ALARMS, repository.observeAlarmProfiles().first())
        }

    @Test
    fun `같은 게스트 세션 안에서는 추가한 알림이 재조회 후에도 유지된다`() =
        runTest {
            guestModeStore.enterGuestMode()
            repository.refreshAlarmProfiles()

            repository.addAlarmProfile(
                stockCode = "035420",
                stockName = "NAVER",
                hour = 10,
                minute = 0,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
            )
            // AlarmDetailViewModel도 init에서 refreshAlarmProfiles를 다시 호출한다.
            repository.refreshAlarmProfiles()

            val alarms = repository.observeAlarmProfiles().first()
            assertEquals(GuestAlarmData.SEED_ALARMS.size + 1, alarms.size)
            assertTrue(alarms.any { it.stockCode == "035420" })
        }

    @Test
    fun `게스트를 재진입하면 세션ID가 바뀌어 초기 시드로 되돌아간다`() =
        runTest {
            guestModeStore.enterGuestMode()
            repository.refreshAlarmProfiles()
            repository.addAlarmProfile(
                stockCode = "035420",
                stockName = "NAVER",
                hour = 10,
                minute = 0,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
            )

            guestModeStore.enterGuestMode() // 재진입 → guestSessionId 증가
            repository.refreshAlarmProfiles()

            assertEquals(GuestAlarmData.SEED_ALARMS, repository.observeAlarmProfiles().first())
        }

    @Test
    fun `토글과 삭제가 인메모리 캐시에 즉시 반영된다`() =
        runTest {
            guestModeStore.enterGuestMode()
            repository.refreshAlarmProfiles()
            val target = GuestAlarmData.SEED_ALARMS.first()

            val toggled = repository.updateAlarmProfile(target.id, !target.isEnabled)

            assertTrue(toggled is NetworkResult.Success)
            assertEquals(
                !target.isEnabled,
                repository.observeAlarmProfiles().first().first { it.id == target.id }.isEnabled,
            )

            repository.deleteAlarmProfile(target.id)

            assertTrue(repository.observeAlarmProfiles().first().none { it.id == target.id })
        }

    @Test
    fun `보유 종목 스트림은 시드 종목 목록을 반환한다`() =
        runTest {
            assertEquals(GuestAlarmData.PORTFOLIO_STOCKS, repository.observePortfolioStocks().first())
        }
}
