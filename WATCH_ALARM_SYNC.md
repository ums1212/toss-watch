# 워치 알람 설정 동기화

## 사용자 흐름

- 워치의 **연동 완료 → 알람 설정 → 종목 → 알람 추가**에서 한국 시간과 반복 요일을 선택한다.
- 종목별 알람 켜기/끄기와 삭제를 지원한다. 삭제 전 확인을 받는다.
- 종목은 폰 대시보드가 마지막으로 조회한 선택 계좌의 Room 캐시를 사용한다. 계좌를 바꾸거나 포트폴리오를 새로고침하면 워치로 다시 전달된다. 워치의 새로고침은 알람 목록을 서버에서 조회하며, 계좌 선택을 바꾸지는 않는다.
- 매도한 종목도 알람이 남아 있으면 별도 목록에 표시한다. 해당 알람은 끄거나 삭제할 수 있다.
- 폰과 워치 **두 앱을 모두 업데이트**해야 한다. Android 시스템에서 페어링된 기기여야 하며, 두 앱의 applicationId와 서명이 같아야 한다. QR 연동은 서버의 사용자↔워치 연결이며, 시스템 페어링을 대신하지 않는다.

## 구성

- `core:model/.../watch/WatchAlarmSync.kt`: 버전이 있는 직렬화 계약. JWT, 토스 API 키는 전송하지 않는다.
- 폰 `watchsync/PhoneAlarmSyncBridge`: 기존 알람 UseCase와 포트폴리오 캐시를 관찰하고 스냅샷을 전송한다. 워치 요청은 `WorkManager`가 네트워크 연결 후 처리한다.
- 워치 `WatchAlarmRepositoryImpl`: Preferences DataStore에 Data Layer 수신 스냅샷과 대기 요청을 보관한다. UI는 UseCase를 통해서만 접근한다.
- `/alarm-sync/v1/request/{uuid}`: 워치 소유 요청 DataItem.
- `/alarm-sync/v1/snapshot/{uuid}`: 폰 소유 목록 및 처리 결과 DataItem.
- 알람 설정 동기화는 새 백엔드 엔드포인트 없이 기존 JWT 인증 및 자동 갱신을 사용한다.

## 알람 발화 (워치 로컬 AlarmManager)

서버 FCM 푸시 대신 워치가 동기화된 스냅샷을 기준으로 직접 알람을 예약한다(이슈 #2, Play `USE_FULL_SCREEN_INTENT` 정책 대응).

- `RescheduleStockAlarmsUseCase`: 스냅샷의 켜진 알람을 `nextTriggerAt`(한국 시간, 0=월..6=일)으로 계산해 `AndroidStockAlarmScheduler`가 `AlarmManager.setAlarmClock`으로 예약한다. 사라지거나 꺼진 알람은 예약 해제한다(예약 id는 DataStore `stock_alarm_scheduled_ids`에 보관). 멱등.
- 재예약 시점: 스냅샷 변경(`WatchApplication` 관찰 + `WatchAlarmSyncService` 수신 직후), 알람 발화 직후(다음 회차), 재부팅·시간/시간대 변경·앱 업데이트(`StockAlarmRescheduleReceiver`).
- 발화: `StockAlarmReceiver`가 알람이 여전히 켜져 있는지 확인하고 `CATEGORY_ALARM` 전체 화면 알림을 띄운다(Android 14+에서 전체 화면 권한이 꺼져 있으면 heads-up으로 폴백).
- `StockAlarmActivity`: “오늘의 OO 주 정보가 도착했습니다.” 화면에서 진동(최대 1분)을 울리며, 뜨는 즉시 `POST /watch/stock-quote/`(API 명세 2-6)로 시세를 미리 조회한다. 사용자가 누르면 시세 화면을 보여주고, 응답 전이면 프로그래스바, 실패 시 재시도 버튼을 표시한다.
- 권한: `USE_EXACT_ALARM`(API 33+, 알람 앱 전용 — Play 선언 필요), `SCHEDULE_EXACT_ALARM`(maxSdk 32), `RECEIVE_BOOT_COMPLETED`, `USE_FULL_SCREEN_INTENT`.
- FCM은 페어링 식별(QR의 토큰, `fcm-token/check`)에만 쓴다. 메시지 수신 서비스(`FirebaseMessagingService`)는 제거했다.

## 오프라인과 정합성

- 이미 동기화한 종목은 연결이 끊겨도 조회하고 알람을 작성할 수 있다. 초기 종목 수신에는 폰 연결이 필요하다.
- 한 번에 **하나의 변경 요청**을 보관한다. 대기 중인 읽기 요청은 변경 요청으로 대체할 수 있다. 수정 사항은 서버의 성공 응답 전까지 목록에 낙관적으로 반영하지 않는다.
- DataClient가 기기 간 전달을 보관하고, 폰 WorkManager가 수신 후 네트워크 작업을 보관한다. 재시도 버튼은 동일 요청 ID를 재전송한다.
- 폰은 실행 전에 요청 ID에 `UNKNOWN` 영수증을 영속화한다. 성공 후 `SAVED`, 명확한 거절은 `FAILED`로 바꾼다. 재전달된 ID는 결과만 재전송한다.
- 서버에 idempotency key API가 없으므로 HTTP 타임아웃/프로세스 종료 직후 결과를 확정할 수 없는 경우 자동으로 POST를 반복하지 않는다. 워치에서 결과 불명 안내를 표시하고 새로고침 및 폰 확인을 요청한다.
- 로그인 세션·워치 UUID·연동 시각이 달라지면 동기화 세션도 바뀐다. 이전 세션의 변경 요청은 거절한다. 처리 전 서버의 현재 워치 등록 정보도 조회한다. 게스트 세션은 실제 워치에 동기화하지 않는다.
- 스냅샷에는 단조 증가 revision을 사용한다. 순서가 뒤바뀐 수신 이벤트는 최신 목록/대기 요청을 덮어쓰지 않는다.
- 전송 크기를 90 KB로 제한한다. 초과 시 보유 종목 목록만 빼고 알람 목록은 보내(워치 로컬 알람은 계속 울림) 워치 편집을 막고 폰에서 관리하도록 안내한다. 알람만으로도 초과할 때에만 둘 다 비운다(`fitSnapshotToLimit`).

## 검증

JDK 17을 지정한 저장소 루트에서:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :app:assembleDebug :watch-app:assembleDebug :app:testDebugUnitTest :watch-app:testDebugUnitTest :app:lintDebug :watch-app:lintDebug
```

단위 테스트는 재전달/재시작/동시 요청에서 POST 중복 방지, 타임아웃 결과, 중복 탭, 요일/시간 검증, 입력 복원, 오프라인 편집, 요청 ID와 세션에 따른 수신 처리를 다룬다.

2026-09-12 검증 결과: 위 명령 전체 성공. 신규 테스트 18개와 기존 앱 테스트 1개 통과. 폰/워치 디버그 APK 생성 및 lint 오류 0개(경고는 남아 있음).

실기기 확인 항목:

1. 동일 서명으로 폰과 워치를 설치하고 시스템 페어링 및 QR 연동을 완료한다.
2. 폰의 계좌를 새로고침한 뒤 워치에서 종목·알람 목록을 확인한다. 다른 계좌 선택 시 목록도 바뀌는지 확인한다.
3. 워치에서 알람 추가/끄기/켜기/삭제를 실행해 폰 목록과 서버 결과가 일치하는지 확인한다. 시간 선택, 요일 선택, 스와이프/시스템 뒤로가기도 확인한다.
4. 폰에서 알람을 변경하고 워치로 반영되는지 확인한다.
5. 연결을 끊고 워치에서 저장한 뒤 재연결한다. 대기 표시가 해제되고 알람이 한 건만 생성되는지 확인한다.
6. 폰 프로세스를 종료한 상태에서도 수신 후 처리가 되는지 확인한다. 강제 중지는 Android가 백그라운드 작업을 차단하므로 앱을 다시 실행한 뒤 확인한다.
7. 저장 대기 중 로그아웃/다른 계정 재연동 시 이전 요청이 새 계정에 적용되지 않는지 확인한다.

이 작업 환경에는 연결된 워치/에뮬레이터가 없어 실기기 간 전송과 원형 화면의 시각 검증은 수행하지 못했다.

참고: [Data Layer 개요](https://developer.android.com/training/wearables/data/overview), [DataClient 동기화](https://developer.android.com/training/wearables/data/data-items), [클라이언트별 전달 특성](https://developer.android.com/training/wearables/data/client-types).
