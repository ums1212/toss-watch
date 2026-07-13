package dev.comon.toss_watch.core.datastore.crypto

import com.google.crypto.tink.Aead
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tink AEAD(AES256-GCM)로 토큰 문자열을 암/복호화하는 얇은 래퍼.
 *
 * 키셋은 Android Keystore 마스터키로 감싸여 저장되므로(=키가 앱 프로세스 밖으로 노출되지 않음),
 * DataStore에는 항상 이 클래스를 거친 Base64 암호문만 기록된다.
 */
@Singleton
internal class TokenCipher @Inject constructor(
    private val aead: Aead,
) {

    fun encrypt(plainText: String): String {
        val cipherBytes = aead.encrypt(plainText.encodeToByteArray(), ASSOCIATED_DATA)
        return Base64.getEncoder().encodeToString(cipherBytes)
    }

    /** 복호화 실패(키 유실, 데이터 변조 등) 시 [GeneralSecurityException]을 던진다. */
    @Throws(GeneralSecurityException::class)
    fun decrypt(cipherText: String): String {
        val plainBytes = aead.decrypt(Base64.getDecoder().decode(cipherText), ASSOCIATED_DATA)
        return plainBytes.decodeToString()
    }

    private companion object {
        // 다른 용도의 암호문이 토큰 자리에 이식(replay)되는 것을 막는 컨텍스트 바인딩 값.
        val ASSOCIATED_DATA: ByteArray = "toss_watch_session_token".encodeToByteArray()
    }
}
