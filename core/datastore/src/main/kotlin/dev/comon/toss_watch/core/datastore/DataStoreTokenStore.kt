package dev.comon.toss_watch.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.comon.toss_watch.core.datastore.crypto.TokenCipher
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Jetpack Preferences DataStore + Tink AEAD 조합의 [TokenStore] 구현체.
 *
 * 저장 파이프라인: JWT 평문 → [TokenCipher] (Keystore 마스터키 기반 AES256-GCM) → Base64 → DataStore.
 *
 * `runBlocking`을 쓰는 이유: 이 저장소의 소비자는 OkHttp의 Interceptor/Authenticator로,
 * OkHttp 워커 스레드에서 동기적으로 호출된다(메인 스레드 아님). suspend 계약으로 바꾸면
 * 결국 네트워크 레이어에서 runBlocking이 필요해지므로 블로킹 지점을 이 모듈 안에 캡슐화한다.
 */
@Singleton
internal class DataStoreTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val tokenCipher: TokenCipher,
) : TokenStore {

    override fun observeHasSession(): Flow<Boolean> =
        dataStore.data
            // 복호화 없이 키 존재 여부만 판단 — 라우팅 분기에 토큰 평문이 필요 없다.
            .map { prefs -> prefs.contains(KEY_REFRESH_TOKEN) }
            .distinctUntilChanged()

    override fun getAccessToken(): String? = readToken(KEY_ACCESS_TOKEN)

    override fun getRefreshToken(): String? = readToken(KEY_REFRESH_TOKEN)

    override fun saveTokens(accessToken: String, refreshToken: String) {
        runBlocking {
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = tokenCipher.encrypt(accessToken)
                prefs[KEY_REFRESH_TOKEN] = tokenCipher.encrypt(refreshToken)
            }
        }
    }

    override fun updateAccessToken(accessToken: String) {
        runBlocking {
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = tokenCipher.encrypt(accessToken)
            }
        }
    }

    override fun clear() {
        runBlocking {
            dataStore.edit { prefs ->
                prefs.remove(KEY_ACCESS_TOKEN)
                prefs.remove(KEY_REFRESH_TOKEN)
            }
        }
    }

    private fun readToken(key: Preferences.Key<String>): String? = runBlocking {
        val encrypted = dataStore.data.first()[key] ?: return@runBlocking null
        try {
            tokenCipher.decrypt(encrypted)
        } catch (e: GeneralSecurityException) {
            // 키 유실/데이터 변조로 복호화 불가 → 세션 없음으로 취급해 재로그인을 유도한다.
            null
        }
    }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")
    }
}
