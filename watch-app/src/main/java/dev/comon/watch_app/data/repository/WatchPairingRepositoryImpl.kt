package dev.comon.watch_app.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.watch_app.data.local.PairingPreferences
import dev.comon.watch_app.data.remote.WatchApi
import dev.comon.watch_app.data.remote.dto.FcmTokenCheckRequest
import dev.comon.watch_app.data.remote.watchSafeApiCall
import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import dev.comon.watch_app.diagnostics.StartupTiming

@Singleton
class WatchPairingRepositoryImpl @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val pairingPreferences: PairingPreferences,
    private val watchApi: WatchApi,
) : WatchPairingRepository {

    override suspend fun getOrCreateDeviceUuid(): String =
        pairingPreferences.getOrCreateDeviceUuid()

    override suspend fun getFcmToken(): Result<String> =
        tokenResult { firebaseMessaging.token.await() }

    override suspend fun refreshFcmToken(): Result<String> =
        tokenResult {
            StartupTiming.measure("fcm.delete") { firebaseMessaging.deleteToken().await() }
            StartupTiming.measure("fcm.issue") { firebaseMessaging.token.await() }
        }

    private suspend fun tokenResult(block: suspend () -> String): Result<String> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkFcmTokenRegistered(fcmToken: String): NetworkResult<Boolean> =
        watchSafeApiCall { watchApi.checkFcmToken(FcmTokenCheckRequest(fcmToken = fcmToken)) }
            .map { it.registered }

    override suspend fun isPaired(): Boolean = pairingPreferences.isPaired()

    override suspend fun setPaired(paired: Boolean) {
        pairingPreferences.setPaired(paired)
    }
}
