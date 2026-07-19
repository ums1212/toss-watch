package dev.comon.toss_watch.core.datastore

import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import kotlinx.coroutines.flow.Flow

/**
 * 세션 토큰(Access/Refresh JWT) 및 온보딩 상태의 보안 저장소 계약.
 *
 * `:core:network`의 인터셉터/Authenticator가 이 인터페이스에만 의존하고,
 * 실제 암호화 구현([DataStoreTokenStore])은 이 모듈 내부에 캡슐화한다.
 */
interface TokenStore {

    /**
     * 세션(Refresh JWT) 존재 여부의 반응형 스트림.
     * :app 최상위 라우터가 구독해 Auth/Dashboard 루트를 동적으로 전환한다.
     * 토큰 평문은 흘리지 않고 존재 여부만 노출한다.
     */
    fun observeHasSession(): Flow<Boolean>

    /**
     * 토스 API 키 등록 여부의 반응형 스트림.
     * 로그인 응답(`has_toss_key`)과 키 등록 성공 이벤트로 갱신되며,
     * :app 최상위 라우터가 구독해 토스 키 입력/대시보드 루트를 전환한다.
     */
    fun observeTossKeyRegistered(): Flow<Boolean>

    fun setTossKeyRegistered(registered: Boolean)

    /**
     * 연동 완료된 워치(기기명 + UUID)의 반응형 스트림.
     * 서버 `GET /users/fcm-token/`은 등록 여부(Boolean)만 반환하므로,
     * 폰이 QR 스캔 후 등록 성공(200) 시점에 저장한 값을 그대로 관측한다.
     * 미연동 상태면 `null`.
     */
    fun observePairedWatch(): Flow<PairedWatchInfo?>

    fun setPairedWatch(modelName: String, uuid: String)

    fun clearPairedWatch()

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun saveTokens(accessToken: String, refreshToken: String)

    fun updateAccessToken(accessToken: String)

    fun clear()
}
