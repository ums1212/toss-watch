package dev.comon.toss_watch.watchsync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.model.watch.WatchAlarmReceipt
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.alarmSyncStore by preferencesDataStore("phone_alarm_sync")

/** Durable request receipts prevent a re-delivered DataItem from creating a second alarm. */
@Singleton
class PhoneAlarmSyncStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val store get() = context.alarmSyncStore
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun session(fingerprint: String, uuid: String): String {
        val prefs = store.data.first()
        if (prefs[FINGERPRINT] == fingerprint) return requireNotNull(prefs[SESSION])
        val session = UUID.randomUUID().toString()
        store.edit {
            val revision = it[REVISION] ?: 0L
            it.clear()
            it[REVISION] = revision
            it[FINGERPRINT] = fingerprint
            it[SESSION] = session
            it[UUID_KEY] = uuid
        }
        return session
    }

    suspend fun previousUuid(): String? = store.data.first()[UUID_KEY]
    suspend fun previousSession(): String? = store.data.first()[SESSION]

    suspend fun clear() { store.edit {
        val revision = it[REVISION] ?: 0L
        it.clear()
        it[REVISION] = revision
    } }

    suspend fun nextRevision(): Long {
        val prefs = store.edit { it[REVISION] = maxOf((it[REVISION] ?: 0L) + 1, System.currentTimeMillis()) }
        return requireNotNull(prefs[REVISION])
    }

    suspend fun receipt(id: String): WatchAlarmReceipt? = store.data.first()[receiptKey(id)]?.let {
        json.decodeFromString(WatchAlarmReceipt.serializer(), it)
    }

    suspend fun lastReceipt(): WatchAlarmReceipt? = store.data.first()[LAST_RECEIPT]?.let { receipt(it) }

    suspend fun saveReceipt(receipt: WatchAlarmReceipt) {
        store.edit {
            it[receiptKey(receipt.requestId)] = json.encodeToString(WatchAlarmReceipt.serializer(), receipt)
            it[LAST_RECEIPT] = receipt.requestId
        }
    }

    private fun receiptKey(id: String) = stringPreferencesKey("receipt_$id")

    private companion object {
        val FINGERPRINT = stringPreferencesKey("fingerprint")
        val SESSION = stringPreferencesKey("session")
        val UUID_KEY = stringPreferencesKey("uuid")
        val LAST_RECEIPT = stringPreferencesKey("last_receipt")
        val REVISION = longPreferencesKey("revision")
    }
}
