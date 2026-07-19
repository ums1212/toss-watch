package dev.comon.toss_watch.core.model.watch

/**
 * 폰앱(feature:setting)에 저장되는, 연동 완료된 워치 정보.
 *
 * 서버 `GET /api/v1/toss-watch/users/fcm-token/`는 등록 여부(Boolean)만 반환하므로
 * 모델명·UUID는 폰이 등록 성공(200) 시점에 직접 로컬(core:datastore)에 저장해 관측한다.
 */
data class PairedWatchInfo(
    val modelName: String,
    val uuid: String,
)
