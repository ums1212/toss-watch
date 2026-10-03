package dev.comon.toss_watch.feature.setting.domain.repository

import android.graphics.Bitmap
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImage
import dev.comon.toss_watch.feature.setting.domain.model.AlarmImageCrop
import kotlinx.coroutines.flow.Flow

/**
 * 워치 알람 이미지의 폰 로컬 저장소. 백엔드를 거치지 않는 기기 로컬 설정이라 게스트 모드 라우팅이 없다 —
 * 워치로의 전송은 :app의 `PhoneAlarmImageSync`가 [observe]를 구독해 처리한다.
 */
interface AlarmImageRepository {

    /** 모든 슬롯의 현재 설정을 [WatchAlarmImageSlot] 선언 순서로 방출한다. */
    fun observe(): Flow<List<AlarmImage>>

    /** 이미지 피커가 돌려준 [sourceUri]를 편집용 비트맵으로 읽는다(EXIF 회전 반영). 실패하면 `null`. */
    suspend fun decode(sourceUri: String): Bitmap?

    /** [source]를 [crop] 영역으로 잘라 [slot]의 이미지로 저장한다. 저장에 실패하면 `false`. */
    suspend fun set(slot: WatchAlarmImageSlot, source: Bitmap, crop: AlarmImageCrop): Boolean

    /** [slot]을 기본 이미지로 되돌린다. */
    suspend fun reset(slot: WatchAlarmImageSlot)
}
