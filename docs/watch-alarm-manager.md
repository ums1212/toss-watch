# 워치 알람: FCM 푸시에서 AlarmManager로

워치앱(`:watch-app`)의 주식 알람을 **서버 FCM 푸시**에서 **워치 로컬 `AlarmManager`**로 전환한 구조와 그 이유를 정리한다.
관련 이슈: [#2](https://github.com/ums1212/toss-watch/issues/2).

---

## 1. 왜 바꿨나

- Play 심사가 `USE_FULL_SCREEN_INTENT` 사용을 반복해서 거절했다. 이 권한은 **핵심 기능이 알람 시계이거나 통화인 앱**만 쓸 수 있는데,
  서버 푸시를 받아 전체 화면을 띄우는 동작은 "알림"으로 판정된다.
- 사용자가 요일·시각을 지정해 두고 그 시각에 기기가 스스로 울리는 구조여야 "알람 앱"으로 설명할 수 있다.

부수 효과로 알람 자체는 네트워크 없이도 울린다. 시세 조회에만 네트워크가 필요하다.

---

## 2. 전체 흐름

```
[폰] 알람 추가/수정/삭제 → 서버 저장(POST /notifications/) → 알람 목록 갱신
        │
        └─ Data Layer로 전체 목록(WatchAlarmSnapshot) 전송
                 │
[워치] 스냅샷 수신 → RescheduleStockAlarmsUseCase → AlarmManager.setAlarmClock 예약
                 │
            알람 시각 도달
                 │
        StockAlarmReceiver
          ├─ 웨이크락으로 화면 깨우기
          ├─ 전체 화면 알람 알림 발행(폴백)
          ├─ StockAlarmActivity 직접 실행
          └─ 같은 알람의 다음 회차 재예약
                 │
        StockAlarmActivity
          ├─ "오늘의 OO 주 정보가 도착했습니다." + 반복 진동(최대 1분)
          ├─ 즉시 시세 prefetch(POST /watch/stock-quote/)
          └─ 사용자가 누르면 시세 화면(응답 전이면 로딩, 실패 시 재시도)
```

알람 목록의 원천은 서버이며, 워치는 폰에서 받은 스냅샷을 **로컬 예약의 기준**으로만 쓴다. 워치에서 알람을 추가/수정/삭제해도
폰이 같은 서버 API를 대신 호출한 뒤 갱신된 목록을 다시 내려준다.

---

## 3. 구성 요소

| 파일 | 역할 |
|---|---|
| `domain/alarm/NextAlarmTime.kt` | `WatchAlarm.nextTriggerAt(now)` — 다음 발화 시각 계산(순수 함수) |
| `domain/repository/StockAlarmScheduler.kt` | 예약/해제 인터페이스 (`scheduledIds`, `schedule`, `cancel`) |
| `data/alarm/AndroidStockAlarmScheduler.kt` | `AlarmManager.setAlarmClock` 구현, 예약 id를 DataStore에 보관 |
| `domain/usecase/StockAlarmUseCases.kt` | `RescheduleStockAlarmsUseCase`(멱등 재예약), `FindWatchAlarmUseCase`, `FetchStockQuoteUseCase` |
| `service/StockAlarmReceiver.kt` | 알람 발화 처리 — 화면 깨우기, 알림, 액티비티 실행, 다음 회차 재예약 |
| `service/StockAlarmRescheduleReceiver.kt` | 재부팅·시간/시간대 변경·앱 업데이트 시 전체 재예약 |
| `service/StockAlarmNotifications.kt` | 알람 알림(채널, 전체 화면 인텐트, 알람 Intent) |
| `presentation/alarm/StockAlarm*.kt` | 알람 화면(MVI): 울림 화면 → 시세 화면(로딩/에러 포함) |
| `di/StockAlarmEntryPoint.kt` | BroadcastReceiver에서 UseCase를 꺼내 쓰는 Hilt EntryPoint |

`@AndroidEntryPoint` 리시버는 Hilt Gradle 플러그인의 바이트코드 변환 때문에 Kotlin에서 추상 `super.onReceive()`를 호출해야 하는
문제가 있어, 리시버는 `EntryPointAccessors`로 의존성을 직접 꺼낸다.

---

## 4. 예약 규칙

- **시간대는 `Asia/Seoul` 고정**(`ALARM_ZONE`). 폰·워치 UI가 모두 "한국 시간"으로 안내하므로 기기 시간대를 따르지 않는다.
- `days`는 `0`=월 ~ `6`=일 (서버 `days_of_week`와 동일). `DayOfWeek.of(day + 1)`로 변환한다.
- 꺼진 알람(`enabled=false`)이나 요일이 없는 알람은 예약하지 않는다.
- `setAlarmClock`을 쓰는 이유: Doze 상태에서도 정시에 깨어나고, 시스템에 "알람 시계"로 표시된다.
- 예약한 알람 id는 DataStore(`stock_alarm_scheduled_ids`)에 남긴다. 프로세스가 재시작돼도 폰에서 삭제된 알람의 예약을 해제할 수 있다.
- `RescheduleStockAlarmsUseCase`는 멱등이다. 스냅샷의 켜진 알람을 모두 (재)예약하고, 목록에서 사라졌거나 꺼진 알람은 해제한다.

**재예약 시점**

| 시점 | 트리거 |
|---|---|
| 알람 목록 변경 | `WatchApplication`이 스냅샷 변화를 관찰 + `WatchAlarmSyncService` 수신 직후 |
| 알람 발화 직후 | `StockAlarmReceiver` (같은 알람의 다음 회차) |
| 재부팅·시간/시간대 변경·앱 업데이트 | `StockAlarmRescheduleReceiver` |

발화 직후 재예약은 기준 시각을 "현재 분의 59초"로 잡는다. 방금 울린 회차가 같은 분에 다시 예약되는 것을 막기 위함이다.

---

## 5. 화면이 꺼져 있을 때 깨우기 (가장 어려웠던 부분)

### 이전 FCM 구조에서 막혔던 것

실기기(Samsung Galaxy Watch `SM-R910`, One UI Watch, Android 16)에서 화면이 꺼져 있으면 전체 화면 알림이 뜨지 않았다.
logcat으로 확인한 원인은 다음과 같았다.

```
E ActivityTaskManager: Background activity launch blocked! goo.gle/android-bal
  [callingPackage: dev.comon.watch_app;
   realCallingPackage: com.samsung.android.wearable.sysui;   ← 삼성 시스템 UI가 우리 PendingIntent를 대신 전송
   balAllowedByPiCreator: BSP.NONE; resultIfPiCreatorAllowsBal: BAL_BLOCK; ...]
```

- 화면이 꺼진 상태에서 전체 화면 인텐트를 실제로 "발사"하는 주체는 우리 앱이 아니라 **삼성 One UI Watch의 시스템 UI**다.
- Android 15+에서는 PendingIntent를 다른 앱이 대신 전송할 때 **만든 쪽(creator)도 백그라운드 액티비티 실행을 명시적으로 허용**해야 한다.
  → `ActivityOptions.setPendingIntentCreatorBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)` 적용(API 35+).
- 그래도 최종 결과는 `BAL_BLOCK`이었다. 당시 앱 프로세스는 `CACHED_RECENT` 상태였고, 삼성 sysui도 visible이 아니어서 둘 다 자격 미달이었다.
  삼성 자체 알림 레이어(`WNoti`)의 허용 앱 목록(`allowedMobileAppHashMap`)에 우리 앱이 없다는 로그도 함께 찍혔다.
- 시도했다가 되돌린 것: `shortService` 타입 포그라운드 서비스로 승격(승격 자체는 성공했지만 `USE_FULL_SCREEN_INTENT` AppOps 거부와 BAL 차단 어느 쪽도 풀리지 않았다).

이 시점의 결론은 "이 기기에서는 앱 코드로 해결 불가"였다. **AlarmManager로 바꾸면서 전제가 달라졌다.**

### 현재 구조에서 해결한 방법

알람은 이제 OEM 알림 레이어가 아니라 **우리 앱의 리시버가 직접** 받는다. 그래서 `StockAlarmReceiver`가 순서대로 처리한다.

1. **화면 깨우기** — `SCREEN_BRIGHT_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP` 웨이크락을 짧게(10초) 잡는다.
   기기가 자고 있으면 액티비티가 떠도 사용자가 볼 수 없으므로 이 단계가 핵심이다. 액티비티가 뜬 뒤에는 `FLAG_KEEP_SCREEN_ON`이 화면을 유지한다.
2. **알림 발행** — `CATEGORY_ALARM` + 전체 화면 인텐트. 3번이 막히는 기기에서도 알림이 폴백으로 남는다.
   Android 14+에서 사용자가 전체 화면 알림 권한을 껐다면(`canUseFullScreenIntent()`) heads-up 알림으로만 울린다.
3. **액티비티 직접 실행** — `context.startActivity(...)`. `SecurityException`은 잡아서 로그만 남긴다(알림 폴백이 이미 있음).

**검증 결과(2026-09-23, `SM-R910`)**: 슬립 상태에서 알람 시각에 화면이 켜지고 알람 화면이 바로 표시되는 것을 확인했다.

### 진동

`fullScreenIntent`가 붙은 알림은 Wear OS 플랫폼이 채널에 진동을 설정해도 억제한다(로그: `WearServices StreamManagerCollectorListener`가
`shouldVibrate=false`로 덮어씀). 그래서 채널 진동에 기대지 않고 `StockAlarmActivity`가 직접 `Vibrator`를 반복 재생하고,
사용자가 반응하지 않으면 1분 뒤 멈춘다. 알림 채널 설정은 생성 후 불변이라 설정을 바꿀 때는 채널 ID를 올린다(현재 `stock_alarm_channel_v3`).

---

## 6. 권한

| 권한 | 용도 |
|---|---|
| `USE_EXACT_ALARM` | 정확 알람(API 33+). 설치 시 자동 허용되지만 **알람 시계 앱 전용**이라 Play 선언이 필요하다 |
| `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`) | API 31~32용. 해당 버전에서는 자동 허용 |
| `USE_FULL_SCREEN_INTENT` | 잠금/꺼진 화면 위 알람 표시. Play 선언 필요 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 재예약 |
| `WAKE_LOCK` | 알람 시각에 화면 깨우기 |
| `POST_NOTIFICATIONS` | 알람 알림. 미허용이면 알람이 울리지 않으므로 첫 실행 시 요청한다 |
| `VIBRATE` | 알람 진동 |

Android 14+에서 `SCHEDULE_EXACT_ALARM`은 기본 거부라 사용자가 설정에서 직접 켜야 한다. 그래서 자동 허용되는 `USE_EXACT_ALARM`을 쓴다.
`USE_FULL_SCREEN_INTENT`와 근거가 같으므로(알람 시계 앱) Play 선언도 함께 처리한다.

---

## 7. FCM은 어디까지 남았나

- 남은 것: **페어링 식별용 토큰**뿐이다. 워치가 QR에 담아 보여주는 `fcm_token`, 폰의 등록(2-3), 워치의 등록 확인(2-5)에 쓴다.
- 없앤 것: `FirebaseMessagingService` 구현체(`WatchNotificationService`)와 매니페스트 등록. 푸시 메시지로는 알람을 띄우지 않는다.
- 그래서 서버가 구버전 호환을 위해 FCM을 계속 보내도 신버전 워치앱에서는 **중복 알람이 생기지 않는다**(수신 자체를 하지 않음).

---

## 8. 서버 쪽에서 할 일

1. **시세 조회 API 구현** — `POST /api/v1/toss-watch/watch/stock-quote/` (명세 [2-6](TOSS_WATCH_API_SPEC.md)).
   워치는 알람 화면이 뜨는 즉시 호출하고, 사용자가 화면을 누르면 결과를 보여준다.
   `price`/`change_rate`는 기존 FCM 페이로드와 같은 **문자열**이어야 한다.
2. **FCM 발송 중단 순서** — 신버전 워치앱 배포 후에 `run_watch_scheduler`(매분 배치)를 멈춘다.
   먼저 끄면 아직 업데이트하지 않은 구버전 워치 사용자가 알람을 받지 못한다.
3. **자동 비활성화된 알람 복구** — FCM 토큰 미등록/무효를 사유로 `is_active=false`가 된 알람은 워치에서도 예약되지 않는다.
   발송 중단 시점에 해당 알람의 `is_active`를 되살리고 `disabled_reason`을 비운다.

---

## 9. 폰 동기화와의 관계

- 워치는 폰에서 받은 스냅샷이 있어야 알람을 예약한다. 즉 **폰앱도 동기화 기능이 포함된 버전**이어야 하고, 두 기기가 시스템에서
  페어링되어 있어야 하며, 두 앱의 `applicationId`와 서명이 같아야 한다.
- 폰에서 로그아웃하거나 다른 워치로 다시 연동하면 빈 목록이 전달되어 워치 알람 예약이 모두 해제된다(의도된 동작).
- 네트워크 오류 같은 일시적 실패에서는 워치가 **이전 목록을 유지**한다.
- 스냅샷이 Data Layer 한도(90 KB)를 넘으면 보유 종목 목록만 빼고 **알람 목록은 유지**한다(`fitSnapshotToLimit`).
  워치에서는 편집만 막히고 알람은 계속 울린다. 알람만으로도 한도를 넘는 경우에만 둘 다 비운다.

---

## 10. 테스트

**단위 테스트**

| 테스트 | 내용 |
|---|---|
| `NextAlarmTimeTest` | 오늘 미래/과거 시각, 다음 선택 요일, 일→월 경계, 기기 시간대 무관, 꺼진 알람 |
| `RescheduleStockAlarmsUseCaseTest` | 켜진 알람 예약, 삭제·비활성 알람 해제, 미연동 시 전체 해제 |
| `StockAlarmViewModelTest` | 울리는 동안 prefetch, 응답 전 열면 로딩→시세, 에러 후 재시도, 새 알람 수신 |
| `WatchSnapshotSizeLimitTest` (`:app`) | 한도 초과 시 종목만 제거하고 알람 유지 |

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-17"   # Git Bash
./gradlew :watch-app:testDebugUnitTest :app:testDebugUnitTest :watch-app:assembleDebug :watch-app:lintDebug
```

**실기기 확인**

```bash
adb shell dumpsys alarm | grep -A3 dev.comon.toss_watch          # 예약 확인
adb shell appops get dev.comon.toss_watch USE_FULL_SCREEN_INTENT # 전체 화면 권한
adb logcat -s StockAlarmReceiver StockAlarmNotifications ActivityTaskManager
```

1. 폰에서 2~3분 뒤 알람을 추가하고 워치에 목록이 동기화되는지 확인한다.
2. 워치 화면을 끄고 기다린다 → 화면이 켜지고 알람 화면이 떠야 한다. (2026-09-23 확인 완료)
3. 알람 화면을 눌러 시세가 표시되는지 확인한다(서버 API 필요).
4. `adb reboot` 후 예약이 복구되는지 확인한다.
5. 폰에서 알람을 끄거나 삭제한 뒤 예약이 해제되는지 확인한다.
6. `adb shell appops set dev.comon.toss_watch USE_FULL_SCREEN_INTENT deny`로 heads-up 폴백을 확인한다.

---

## 11. Play Console 체크리스트

- 전체 화면 인텐트 선언: "알람 시계" 선택, 사용자가 지정한 요일·시각에 울리는 알람임을 설명
- 정확한 알람(`USE_EXACT_ALARM`) 선언: 같은 근거
- 데모 영상: 알람 추가(시각·요일) → 화면 꺼진 상태에서 전체 화면 알람 → 탭 → 시세
- 워치앱 `versionCode` 증가, `:app`과 **같은 서명 키** 사용
- 리스크: 선언을 해도 Google이 "주식 앱"으로 판단하면 거절될 수 있다. 스토어 설명과 스크린샷에서 요일·시각 지정 알람을 전면에 둔다.
