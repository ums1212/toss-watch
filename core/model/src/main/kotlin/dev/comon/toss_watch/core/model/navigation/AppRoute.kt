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

/**
 * 하단 탭(대시보드/알림)을 감싸는 루트 목적지. 로그인 세션이 있을 때의 루트 목적지.
 * 대시보드와 알림 탭 전환은 이 목적지 내부의 로컬 상태로 처리되며 백스택에 별도로 쌓이지 않는다.
 */
@Serializable
data object BottomMenuRoute : AppRoute

/** 토스 연동 · Wear OS 연동 · 로그아웃 설정. */
@Serializable
data object SettingRoute : AppRoute

/**
 * 종목별 알림 목록 — 대시보드 보유종목 카드 또는 알림 탭의 종목 항목을 탭해 진입한다.
 *
 * @param stockCode 알림을 조회/추가할 종목 코드.
 * @param stockName 상단 타이틀 및 알림 추가 다이얼로그에 표시할 종목명. 알 수 없으면 null.
 */
@Serializable
data class AlarmDetailRoute(val stockCode: String, val stockName: String? = null) : AppRoute

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
