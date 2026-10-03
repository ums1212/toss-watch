package dev.comon.toss_watch.feature.alarm.domain.model

/** 새 알림을 등록할 수 없는 사유 — 서버 호출 전에 클라이언트가 먼저 걸러내는 규칙 위반. */
enum class AlarmAddViolation {
    /** 등록된 알림이 이미 [AlarmProfile.MAX_COUNT]개다. */
    LIMIT_REACHED,

    /** 같은 종목·시각에 요일이 하나라도 겹치는 알림이 이미 있다 — 같은 순간에 두 번 울리게 된다. */
    DUPLICATE,
}
