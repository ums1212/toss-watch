package dev.comon.toss_watch.feature.auth.presentation.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Credential Manager 요청의 3가지 종결 상태. */
sealed interface GoogleCredentialResult {
    data class Success(val idToken: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data class Failure(val message: String?) : GoogleCredentialResult
}

/**
 * Android 표준 Credential Manager로 Google ID Token을 얻어오는 UI 레이어 헬퍼.
 *
 * ViewModel이 Android Context를 알 필요가 없도록 Credential Manager 상호작용을
 * 이 클래스에 격리하고, 결과만 MVI Intent로 환원해 전달한다.
 *
 * @param activityContext 계정 선택 시트를 띄우려면 반드시 Activity Context여야 한다.
 */
class GoogleCredentialClient(private val activityContext: Context) {

    private val credentialManager = CredentialManager.create(activityContext)

    suspend fun requestIdToken(serverClientId: String): GoogleCredentialResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            // 기기에 로그인 이력이 없는 신규 유저도 계정 목록에 표시.
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential

            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                GoogleCredentialResult.Success(idToken)
            } else {
                GoogleCredentialResult.Failure("지원하지 않는 자격 증명 유형이에요.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException: type=${e.type}, message=${e.message}", e)
            GoogleCredentialResult.Failure("기기에서 사용할 수 있는 구글 계정이 없어요.")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "GoogleIdTokenParsingException: ${e.message}", e)
            GoogleCredentialResult.Failure("구글 응답을 해석하지 못했어요.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: type=${e.type}, message=${e.message}", e)
            GoogleCredentialResult.Failure(e.message)
        }
    }

    private companion object {
        const val TAG = "GoogleCredential"
    }
}
