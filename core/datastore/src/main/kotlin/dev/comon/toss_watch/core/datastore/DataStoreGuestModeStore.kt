package dev.comon.toss_watch.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * [GuestModeStore]의 Preferences DataStore 구현체.
 *
 * [DataStoreTokenStore]가 쓰는 것과 동일한 `DataStore<Preferences>` 인스턴스(세션 파일)를 재사용한다 —
 * 게스트 플래그도 "이 기기의 현재 세션 상태" 중 하나이므로 별도 파일을 둘 이유가 없다.
 * 토큰이 아닌 평범한 boolean이라 [dev.comon.toss_watch.core.datastore.crypto.TokenCipher] 암호화 대상이 아니다.
 *
 * `runBlocking`을 쓰는 이유는 [DataStoreTokenStore]와 동일 — 호출자(UseCase)가 이미
 * `dispatcherProvider.io`에서 실행 중이므로 이 안에서 블로킹해도 메인 스레드를 막지 않는다.
 */
@Singleton
internal class DataStoreGuestModeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : GuestModeStore {

    // 더미 인메모리 캐시(알림 등)의 재시딩 트리거이므로 프로세스 수명과 일치하는 인메모리 카운터로 둔다 —
    // 앱을 완전히 종료했다가 다시 켜면 더미 데이터도 초기 시드로 돌아가는 것이 의도된 동작이다.
    private val sessionIdCounter = AtomicLong(0)

    override val guestSessionId: Long
        get() = sessionIdCounter.get()

    override fun observeGuestMode(): Flow<Boolean> =
        dataStore.data
            .map { prefs -> prefs[KEY_GUEST_MODE] ?: false }
            .distinctUntilChanged()

    override fun isGuestMode(): Boolean = runBlocking {
        dataStore.data.first()[KEY_GUEST_MODE] ?: false
    }

    override fun enterGuestMode() {
        sessionIdCounter.incrementAndGet()
        runBlocking {
            dataStore.edit { prefs -> prefs[KEY_GUEST_MODE] = true }
        }
    }

    override fun exitGuestMode() {
        runBlocking {
            dataStore.edit { prefs -> prefs.remove(KEY_GUEST_MODE) }
        }
    }

    private companion object {
        val KEY_GUEST_MODE = booleanPreferencesKey("guest_mode")
    }
}
