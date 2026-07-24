package dev.comon.toss_watch.core.model.navigation

import kotlinx.serialization.Serializable

/**
 * 앱 전역 네비게이션 목적지의 컴파일 타임 계약 (Navigation 3).
 *
 * 문자열 기반 라우팅은 금지 — :app의 NavDisplay 백스택은 오직 이 sealed 타입만 담는다.
 * sealed interface이므로 entryProvider의 `when`/`entry<T>` 분기가 exhaustive하게 검증된다.
 */
@Serializable
sealed interface AppRoute

/** 구글 소셜 로그인 랜딩. 세션이 없을 때의 루트 목적지. */
@Serializable
data object AuthRoute : AppRoute

/** 자산/관심 종목 대시보드. 로그인 세션이 있을 때의 루트 목적지. */
@Serializable
data object DashboardRoute : AppRoute

/**
 * 알림 스케줄러 설정.
 *
 * @param prefillStockCode 대시보드 보유종목 카드를 탭해 진입한 경우, 알림 추가 다이얼로그에
 *   미리 선택해 둘 종목 코드. 설정 아이콘을 통한 일반 진입 시엔 null.
 */
@Serializable
data class SettingRoute(val prefillStockCode: String? = null) : AppRoute

/**
 * 토스증권 Open API 키(client_id/client_secret) 등록.
 * 로그인 직후 미등록 상태(has_toss_key=false)일 때의 루트 목적지이자,
 * 설정 화면에서 재등록을 위해 진입할 수도 있는 목적지.
 */
@Serializable
data object TossKeyRoute : AppRoute

/**
 * Wear OS 페어링 — 워치 온보딩 화면에 표시된 QR(FCM 토큰)을 카메라로 스캔해 등록한다.
 * 설정 화면에서 진입하는 목적지.
 */
@Serializable
data object WatchPairRoute : AppRoute
