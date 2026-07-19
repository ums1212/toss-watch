package dev.comon.watch_app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 워치 기기를 식별하는 UUID를 최초 실행 시 1회 발행해 영속화한다.
 * 폰앱과 달리 세션 토큰이 아닌 단순 식별자이므로, `:core:datastore`의 Tink AEAD 암호화 없이
 * 워치앱 전용 평문 DataStore에 저장한다.
 */
@Singleton
class PairingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun getOrCreateDeviceUuid(): String {
        val existing = dataStore.data.first()[KEY_DEVICE_UUID]
        if (existing != null) return existing

        val generated = UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[KEY_DEVICE_UUID] = generated }
        return generated
    }

    private companion object {
        val KEY_DEVICE_UUID = stringPreferencesKey("device_uuid")
    }
}
