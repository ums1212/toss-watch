package dev.comon.toss_watch.feature.dashboard.data.repository

import dev.comon.toss_watch.feature.dashboard.util.FakeDashboardRepository
import dev.comon.toss_watch.feature.dashboard.util.FakeGuestModeStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardRepositoryRouterTest {

    private val remote = FakeDashboardRepository()
    private val guest = FakeDashboardRepository()
    private val guestModeStore = FakeGuestModeStore()
    private val router = DashboardRepositoryRouter(remote, guest, guestModeStore)

    @Test
    fun `게스트 모드가 아니면 remote 리포지토리로만 위임한다`() =
        runTest {
            router.fetchAccounts()
            router.fetchPortfolio(accountSeq = null)

            assertEquals(1, remote.accountsInvocationCount)
            assertEquals(1, remote.portfolioInvocationCount)
            assertEquals(0, guest.accountsInvocationCount)
            assertEquals(0, guest.portfolioInvocationCount)
        }

    @Test
    fun `게스트 모드면 guest 리포지토리로만 위임한다`() =
        runTest {
            guestModeStore.enterGuestMode()

            router.fetchAccounts()
            router.fetchPortfolio(accountSeq = 123L)

            assertEquals(0, remote.accountsInvocationCount)
            assertEquals(0, remote.portfolioInvocationCount)
            assertEquals(1, guest.accountsInvocationCount)
            assertEquals(1, guest.portfolioInvocationCount)
        }

    @Test
    fun `게스트 모드를 이탈하면 다시 remote 리포지토리로 위임한다`() =
        runTest {
            guestModeStore.enterGuestMode()
            router.fetchAccounts()

            guestModeStore.exitGuestMode()
            router.fetchAccounts()

            assertEquals(1, remote.accountsInvocationCount)
            assertEquals(1, guest.accountsInvocationCount)
        }
}
