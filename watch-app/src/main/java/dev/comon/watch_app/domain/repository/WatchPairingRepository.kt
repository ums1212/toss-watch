package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult

interface WatchPairingRepository {

    /** 워치 기기를 식별하는 UUID. 최초 실행 시 1회 발행해 로컬에 영속화한다. */
    suspend fun getOrCreateDeviceUuid(): String

    /** 현재 발급된(캐시된) FCM 등록 토큰. */
    suspend fun getFcmToken(): Result<String>

    /** 기존 토큰을 삭제하고 새로 발급받는다("QR 새로고침"용). */
    suspend fun refreshFcmToken(): Result<String>

    /**
     * 2-5. 로그인 세션 없이 특정 FCM 토큰이 서버 어딘가에 등록되어 있는지 확인한다.
     * `true`면 이미 폰과 연동 완료된 상태.
     */
    suspend fun checkFcmTokenRegistered(fcmToken: String): NetworkResult<Boolean>

    /** 로컬에 저장된 연동 완료 여부. 앱 재실행 시 등록 확인 API 호출을 건너뛰기 위한 캐시. */
    suspend fun isPaired(): Boolean

    /** 연동 완료 여부를 로컬에 영속화한다. */
    suspend fun setPaired(paired: Boolean)
}
