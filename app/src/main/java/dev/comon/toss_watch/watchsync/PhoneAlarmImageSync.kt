package dev.comon.toss_watch.watchsync

import android.content.Context
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSync
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.domain.usecase.ObserveAlarmImagesUseCase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 폰에 저장된 사용자 지정 알람 이미지를 연동된 워치로 보낸다.
 *
 * 이미지가 바뀌거나 워치가 (재)연동될 때마다 슬롯 전체를 DataItem 하나로 다시 올린다 — 내용이 같으면
 * Data Layer가 변경 이벤트를 내지 않으므로 중복 전송 걱정 없이 매번 올려도 된다. 미연동/게스트 상태에서는
 * 보내지 않고, 이미지는 폰에 그대로 남아 있다가 연동되는 시점에 전송된다.
 */
@Singleton
class PhoneAlarmImageSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokens: TokenStore,
    private val guestMode: GuestModeStore,
    private val observeAlarmImages: ObserveAlarmImagesUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(
                tokens.observePairedWatch().map { it?.uuid }.distinctUntilChanged(),
                guestMode.observeGuestMode(),
                observeAlarmImages(),
            ) { uuid, isGuest, images -> if (isGuest || uuid == null) null else uuid to images }
                .collectLatest { target -> target?.let { (uuid, images) -> publish(uuid, images) } }
        }
    }

    private suspend fun publish(uuid: String, images: List<AlarmImage>) {
        try {
            val request = PutDataMapRequest.create(WatchAlarmImageSync.PREFIX + uuid).apply {
                // 사용자 이미지가 없는 슬롯은 키를 넣지 않는다 — 워치는 키가 없으면 기본 이미지로 되돌린다.
                images.forEach { image ->
                    val path = image.customPath ?: return@forEach
                    dataMap.putAsset(image.slot.key, Asset.createFromBytes(File(path).readBytes()))
                }
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Wear API가 없는 기기 등. 다음 앱 시작이나 이미지/연동 변경 때 다시 올린다.
        }
    }
}
