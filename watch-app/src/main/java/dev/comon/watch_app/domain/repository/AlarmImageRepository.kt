package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot

/** 폰에서 받은 사용자 지정 알람 이미지의 워치 로컬 저장소. */
interface AlarmImageRepository {

    /** 사용자 이미지가 설정된 슬롯만 담은, 슬롯 → 이미지 파일 절대 경로. 없는 슬롯은 기본 이미지를 쓴다. */
    suspend fun customImagePaths(): Map<WatchAlarmImageSlot, String>
}
