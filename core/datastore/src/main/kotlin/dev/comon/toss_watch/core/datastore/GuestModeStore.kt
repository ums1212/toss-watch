package dev.comon.toss_watch.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * 게스트(더미 데이터 체험) 모드 상태의 저장소 계약.
 *
 * Google Play 심사 등 실제 로그인 없이 앱을 둘러봐야 하는 상황을 위해, 가짜 토큰을 [TokenStore]에
 * 심는 대신 별도의 플래그로 라우팅을 분기한다 — 가짜 토큰 방식은 `AuthInterceptor`가 그 토큰을 붙여
 * 실제 API 요청을 내보내고, 401 응답이 `TokenAuthenticator`를 거쳐 세션을 지워버리는 부작용이 있다.
 *
 * `:app`의 `MainViewModel.sessionState`가 [TokenStore]의 세션 스트림과 함께 이 스트림을 구독해
 * `SessionState.GUEST`를 판별하고, 각 feature의 Repository 라우터가 [isGuestMode]/[observeGuestMode]를
 * 확인해 실 API 대신 더미 구현으로 위임한다.
 */
interface GuestModeStore {

    /** 게스트 모드 여부의 반응형 스트림 — Repository 라우터가 즉시 구현체를 전환하는 데 사용한다. */
    fun observeGuestMode(): Flow<Boolean>

    /** 게스트 모드 여부의 동기 조회 — suspend 리포지토리 호출 시점의 분기에 사용한다. */
    fun isGuestMode(): Boolean

    /**
     * 게스트 세션 세대 번호. [enterGuestMode]를 호출할 때마다 증가한다.
     * 알림 등 인메모리 더미 캐시가 "이번 게스트 세션에서 이미 시딩했는지"를 판별하는 데 쓰인다 —
     * 같은 세션 안에서는 사용자가 추가/수정한 더미 데이터를 보존하고, 재진입 시에는 초기 시드로 되돌린다.
     */
    val guestSessionId: Long

    fun enterGuestMode()

    fun exitGuestMode()
}
