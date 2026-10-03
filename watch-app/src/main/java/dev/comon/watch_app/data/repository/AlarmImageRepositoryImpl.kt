package dev.comon.watch_app.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSync
import dev.comon.watch_app.data.local.PairingPreferences
import dev.comon.watch_app.domain.repository.AlarmImageRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 폰이 Data Layer로 보낸 알람 이미지를 `filesDir/alarm_images/{up|flat|down}.jpg`에 풀어 둔다.
 * 알람은 폰과 연결이 끊긴 상태에서도 울리므로, 알람 화면은 Data Layer가 아니라 이 로컬 파일만 읽는다.
 */
@Singleton
class AlarmImageRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val pairing: PairingPreferences,
) : AlarmImageRepository {
    private val client = Wearable.getDataClient(context)
    private val directory = File(context.filesDir, "alarm_images")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    /** 앱이 꺼져 있는 동안 놓친 DataItem을 따라잡는다. 실행 중 변경은 `WatchAlarmSyncService`가 전달한다. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            try {
                val items = client.dataItems.await()
                try {
                    for (item in items) accept(item)
                } finally { items.release() }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { /* 다음 실행 때 다시 시도한다. */ }
        }
    }

    override suspend fun customImagePaths(): Map<WatchAlarmImageSlot, String> = withContext(Dispatchers.IO) {
        WatchAlarmImageSlot.entries.map { it to file(it) }
            .filter { (_, file) -> file.exists() }
            .associate { (slot, file) -> slot to file.absolutePath }
    }

    /** [item]이 이 워치 앞으로 온 알람 이미지면 슬롯별로 저장하고, 키가 빠진 슬롯은 기본 이미지로 되돌린다. */
    suspend fun accept(item: DataItem) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (item.uri.path != WatchAlarmImageSync.PREFIX + pairing.getOrCreateDeviceUuid()) return@withContext
            val assets = DataMapItem.fromDataItem(item).dataMap
            for (slot in WatchAlarmImageSlot.entries) {
                val asset = assets.getAsset(slot.key)
                if (asset == null) {
                    file(slot).delete()
                    continue
                }
                try {
                    val response = client.getFdForAsset(asset).await()
                    val bytes = try { response.inputStream.use { it.readBytes() } } finally { response.release() }
                    if (bytes.size > WatchAlarmImageSync.MAX_BYTES || !bytes.isImage()) continue
                    directory.mkdirs()
                    // 알람 화면이 반쯤 쓰인 파일을 읽지 않도록 임시 파일에 쓴 뒤 교체한다.
                    val temp = File(directory, "${slot.key}.tmp")
                    temp.writeBytes(bytes)
                    if (!temp.renameTo(file(slot))) temp.delete()
                } catch (e: CancellationException) { throw e } catch (_: Exception) {
                    // Asset을 아직 내려받지 못한 경우 등 — 이 슬롯은 기존 이미지를 유지한다.
                }
            }
        }
    }

    private fun file(slot: WatchAlarmImageSlot) = File(directory, "${slot.key}.jpg")

    private fun ByteArray.isImage(): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }
}
