package dev.comon.toss_watch.feature.dashboard.util

import dev.comon.toss_watch.core.datastore.GuestModeStore
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 테스트용 [GuestModeStore] 더블. 실제 [dev.comon.toss_watch.core.datastore.DataStoreGuestModeStore]와
 * 동일하게 [enterGuestMode]마다 [guestSessionId]를 증가시켜, 재시딩 로직(예: `GuestAlarmRepository`)을
 * DataStore 없이 검증할 수 있게 한다.
 */
class FakeGuestModeStore : GuestModeStore {

    val guestModeFlow = MutableStateFlow(false)

    private var sessionCounter = 0L

    override fun observeGuestMode() = guestModeFlow

    override fun isGuestMode(): Boolean = guestModeFlow.value

    override val guestSessionId: Long
        get() = sessionCounter

    override fun enterGuestMode() {
        sessionCounter++
        guestModeFlow.value = true
    }

    override fun exitGuestMode() {
        guestModeFlow.value = false
    }
}
