package dev.comon.toss_watch.watchsync

import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot
import dev.comon.toss_watch.core.model.watch.WatchAlarmSync
import kotlinx.serialization.json.Json

/**
 * Data Layer 전송 한도에 맞게 스냅샷을 줄인다.
 *
 * 워치는 스냅샷의 알람 목록으로 로컬 AlarmManager 알람을 예약하므로, 한도를 넘으면 보유 종목 목록만 먼저
 * 빼고 알람은 남긴다(워치에서는 편집만 막히고 알람은 계속 울린다). 알람만으로도 넘칠 때에만 둘 다 비운다.
 */
internal fun fitSnapshotToLimit(
    snapshot: WatchAlarmSnapshot,
    maxBytes: Int = WatchAlarmSync.MAX_BYTES,
): Pair<WatchAlarmSnapshot, ByteArray> {
    fun encode(value: WatchAlarmSnapshot) =
        Json.encodeToString(WatchAlarmSnapshot.serializer(), value).toByteArray()

    val full = encode(snapshot)
    if (full.size <= maxBytes) return snapshot to full

    val alarmsOnly = snapshot.copy(available = false, stocks = emptyList(), error = "TOO_LARGE")
    val alarmsOnlyBytes = encode(alarmsOnly)
    if (alarmsOnlyBytes.size <= maxBytes) return alarmsOnly to alarmsOnlyBytes

    val empty = alarmsOnly.copy(alarms = emptyList())
    return empty to encode(empty)
}
