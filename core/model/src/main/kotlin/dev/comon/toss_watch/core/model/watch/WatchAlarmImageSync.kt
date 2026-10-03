package dev.comon.toss_watch.core.model.watch

/**
 * 사용자 지정 알람 이미지의 Data Layer 계약. 폰이 `PREFIX + 워치 UUID` 경로의 DataItem에
 * 슬롯별 이미지를 Asset([WatchAlarmImageSlot.key])으로 담아 보낸다. 키가 없는 슬롯은 기본 이미지다.
 */
object WatchAlarmImageSync {
    const val PREFIX = "/alarm-image/v1/"

    /** 폰이 크롭해 보내는 정사각형 이미지의 한 변(px). 워치 디스플레이(최대 480px 안팎)를 덮는 크기다. */
    const val IMAGE_SIZE_PX = 480

    /** 워치가 받아들이는 슬롯당 최대 바이트 수. */
    const val MAX_BYTES = 1_000_000
}

/** 워치 알람 이미지 Scene이 시세 방향별로 띄우는 이미지 슬롯. */
enum class WatchAlarmImageSlot(val key: String) {
    UP("up"),
    FLAT("flat"),
    DOWN("down"),
}
