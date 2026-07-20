package dev.comon.toss_watch.core.model.watch

/**
 * 폰앱(feature:setting)에 저장되는, 연동 완료된 워치 정보.
 *
 * 폰이 QR 등록 성공(200) 시점에 직접 로컬(core:datastore)에 저장하거나, 서버
 * `GET /api/v1/toss-watch/users/fcm-token/`(model_name/uuid 포함 응답) 재조회로 복원해 채운다.
 * `modelName`은 워치 등록 시 전달되지 않았다면 서버가 `null`로 저장하므로 nullable이다.
 */
data class PairedWatchInfo(
    val modelName: String?,
    val uuid: String,
)
