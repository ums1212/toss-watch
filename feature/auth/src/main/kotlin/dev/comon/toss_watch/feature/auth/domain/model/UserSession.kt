package dev.comon.toss_watch.feature.auth.domain.model

/**
 * 로그인 성공 후 Domain/Presentation 레이어에 노출되는 세션 정보.
 * JWT 토큰 자체는 Data 레이어에서 :core:datastore로 격리 저장되며
 * 이 모델에는 절대 포함하지 않는다.
 */
data class UserSession(
    val email: String,
    val isNewUser: Boolean,
    val hasTossKey: Boolean,
)
