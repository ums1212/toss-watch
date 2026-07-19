package dev.comon.toss_watch.core.model.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 워치앱 온보딩 QR에 담기는 페이로드.
 *
 * 워치앱이 최초 실행 시 발급한 FCM 토큰과 발행한 기기 UUID, 기기 모델명을
 * JSON으로 직렬화해 QR로 표시하면, 폰앱(feature:setting)이 이를 스캔·역직렬화해
 * `PUT /api/v1/toss-watch/users/fcm-token/` 등록에 사용한다.
 * 두 앱이 서로 의존하지 않도록 공유 지점을 core:model에 둔다.
 */
@Serializable
data class WatchPairingPayload(
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("uuid") val uuid: String,
    @SerialName("model_name") val modelName: String,
)
