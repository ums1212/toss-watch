package dev.comon.toss_watch.feature.setting.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import kotlinx.coroutines.flow.Flow

interface SettingRepository {

    suspend fun registerWatchToken(fcmToken: String, uuid: String, modelName: String): NetworkResult<Unit>

    /**
     * 연동 완료된 워치(기기명+UUID)의 반응형 스트림.
     * QR 등록 성공(200) 시점 또는 [syncPairedWatch] 복원으로 로컬(core:datastore)에
     * 저장해 둔 값을 그대로 관측한다. 미연동이면 `null`.
     */
    fun observePairedWatch(): Flow<PairedWatchInfo?>

    /**
     * 서버 `GET /users/fcm-token/`로 등록 상태를 조회해 로컬(core:datastore) pairedWatch를
     * 서버 기준으로 재동기화한다. 폰앱 재설치 등으로 로컬 값이 유실된 경우 복원하고,
     * 서버가 미등록을 반환하면 로컬 stale 값을 정리한다. best-effort 호출을 전제로 한다.
     */
    suspend fun syncPairedWatch(): NetworkResult<Unit>

    /**
     * 로컬(core:datastore)에 저장된 세션 토큰(Access/Refresh JWT) 및 연동 상태를 모두 제거한다.
     * 게스트 모드였다면 게스트 플래그도 함께 정리한다 — 게스트/실 로그인 어느 쪽이든
     * "로그인 화면으로 돌아간다"는 결과가 같아야 하므로 이 리포지토리 전용 라우팅 대상이 아니다.
     * 서버 측 세션 무효화 API는 없음 — 클라이언트가 토큰을 지우는 즉시
     * :app 최상위 라우터가 `observeHasSession()`을 통해 로그인 화면으로 전환한다.
     */
    fun logout()

    /** 게스트(더미 데이터 체험) 모드 여부의 반응형 스트림 — 설정 화면이 로그아웃/로그인 전환 UI를 고르는 데 쓴다. */
    fun observeGuestMode(): Flow<Boolean>
}
