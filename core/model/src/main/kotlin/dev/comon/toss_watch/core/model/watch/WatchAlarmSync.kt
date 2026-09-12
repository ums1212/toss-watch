package dev.comon.toss_watch.core.model.watch

import kotlinx.serialization.Serializable

/** Versioned Data Layer contract. No credentials are transferred between devices. */
object WatchAlarmSync {
    const val VERSION = 1
    const val REQUEST_PREFIX = "/alarm-sync/v1/request/"
    const val SNAPSHOT_PREFIX = "/alarm-sync/v1/snapshot/"
    const val PAYLOAD = "payload"
    const val MAX_BYTES = 90_000
}

@Serializable
data class WatchStock(val code: String, val name: String)

@Serializable
data class WatchAlarm(
    val id: Long,
    val stockCode: String,
    val stockName: String,
    val hour: Int,
    val minute: Int,
    val days: List<Int>,
    val enabled: Boolean,
    val disabledReason: String = "",
)

@Serializable
enum class WatchAlarmOperation { REFRESH, ADD, TOGGLE, DELETE }

@Serializable
data class WatchAlarmRequest(
    val version: Int = WatchAlarmSync.VERSION,
    val id: String,
    val uuid: String,
    val session: String,
    val operation: WatchAlarmOperation,
    val stockCode: String = "",
    val stockName: String = "",
    val alarmId: Long = 0,
    val hour: Int = 9,
    val minute: Int = 0,
    val days: List<Int> = emptyList(),
    val enabled: Boolean = true,
) {
    fun isValid(): Boolean = version == WatchAlarmSync.VERSION &&
        id.length in 1..100 && uuid.length in 1..100 && session.length <= 100 &&
        when (operation) {
            WatchAlarmOperation.REFRESH -> true
            WatchAlarmOperation.ADD -> stockCode.isNotBlank() && stockCode.length <= 100 &&
                stockName.length <= 200 && hour in 0..23 && minute in 0..59 &&
                days.isNotEmpty() && days.size <= 7 && days.distinct().size == days.size && days.all { it in 0..6 }
            WatchAlarmOperation.TOGGLE, WatchAlarmOperation.DELETE -> alarmId > 0
        }
}

@Serializable
enum class WatchAlarmOutcome { SAVED, FAILED, UNKNOWN }

@Serializable
data class WatchAlarmReceipt(
    val requestId: String,
    val outcome: WatchAlarmOutcome,
    val error: String? = null,
)

@Serializable
data class WatchAlarmSnapshot(
    val version: Int = WatchAlarmSync.VERSION,
    val uuid: String,
    val session: String = "",
    val available: Boolean = false,
    val stocks: List<WatchStock> = emptyList(),
    val alarms: List<WatchAlarm> = emptyList(),
    val receipt: WatchAlarmReceipt? = null,
    val error: String? = null,
    val updatedAt: Long = 0,
)
