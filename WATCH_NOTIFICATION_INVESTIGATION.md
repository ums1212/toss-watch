# 워치앱 알림 기능(헤즈업 + 전체화면 팝업) 구현 기록

목표: 워치 단말기가 꺼져 있는 상태에서도 알람 앱처럼 진동과 함께 화면이 켜지고
`StockAlarmActivity`가 전체화면으로 즉시 표시되도록 만든다.

테스트 기기: Samsung Galaxy Watch (`SM-R910`, One UI Watch, Android 16 / API 36).
프로젝트 targetSdk/compileSdk: 37.

---

## 1. 시작 시점 상태

`:watch-app`은 FCM data-only 페이로드를 받아(`WatchNotificationService : FirebaseMessagingService`)
`IMPORTANCE_HIGH` 채널로 알림을 만들고 `setFullScreenIntent`로 `StockAlarmActivity`를 띄우는
골격은 이미 있었다. 잠금화면 위 표시(`showWhenLocked`/`turnScreenOn`/`requestDismissKeyguard`)도
이미 적용되어 있었다.

**증상**: 워치 화면이 켜져 있을 때는 `StockAlarmActivity`가 뜨는데, 꺼져 있을 때는 뜨지 않음.

---

## 2. 1차 개선 — 진동/화면유지/연속알림/스와이프 (일반적인 Wear OS 모범 사례)

가장 먼저 표준적으로 빠져 있던 부분들을 보완했다.

- **`WatchNotificationService.kt`**
  - `ensureNotificationChannel()`에 `enableVibration(true)` + `vibrationPattern` 명시.
    `NotificationChannel` 설정은 최초 생성 후 불변이므로, 기존 설치 기기에도 반영되도록
    `CHANNEL_ID`를 `stock_alarm_channel` → `stock_alarm_channel_v2`로 변경.
  - Builder에 `.setDefaults(NotificationCompat.DEFAULT_ALL)` + `.setVibrate(...)` 추가.
  - 알림 ID를 고정값 `1001`에서 `stockName.hashCode()`로 변경 → 같은 종목은 갱신, 다른 종목은
    누적되도록 함.
- **`StockAlarmActivity.kt`**
  - `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)` 추가 — 워치는 화면
    자동 꺼짐이 매우 짧아(5~10초), 사용자가 확인하기 전에 꺼지는 것을 방지.
  - `onNewIntent` 오버라이드 추가 — `launchMode="singleInstance"`라 화면이 떠 있는 동안
    새 FCM이 오면 `onCreate`가 아닌 `onNewIntent`로 전달되는데 기존엔 무시되고 있었음.
    `mutableStateOf` 상태를 갱신해 Compose가 최신 종목 정보로 재구성되게 함.
- **`StockAlarmScreen.kt`**: `androidx.wear.compose.material3.SwipeToDismissBox`로 감싸
  좌→우 스와이프 종료 지원(기존 하단 "닫기" 버튼과 병행). 기존 의존성만으로 가능, 신규 의존성 불필요.
- **`AndroidManifest.xml`**: `android.permission.VIBRATE` 추가 — `setVibrate()`/
  `DEFAULT_ALL`(`DEFAULT_VIBRATE` 포함) 경로를 쓰려면 `Notification.vibrate` 필드에 필요한
  권한이라 필수로 판단.

이 단계에서는 실기기 재현 없이 코드 리뷰/공식 문서 검색만으로 진행했다. 컴파일 및 워치 설치까지 확인.

---

## 3. 실기기 진단 1차 — AppOps: `USE_FULL_SCREEN_INTENT` Reject

adb로 연결된 실제 워치에서 `dumpsys package` / `dumpsys appops` / `dumpsys notification`을 확인.

- `USE_FULL_SCREEN_INTENT` 매니페스트 권한은 `granted=true`.
- 하지만 `dumpsys appops`에서:
  ```
  USE_FULL_SCREEN_INTENT (default):
    Reject: [bg-s]2026-07-21 22:47:00.615 (-...)
  ```
  거부 시각이 알림 발행 시각과 정확히 일치. `[bg-s]` = 프로세스가 **background** 상태일 때
  거부됐다는 태그.

**가설 1 (틀림으로 판명)**: FCM 수신 시 프로세스 중요도가 낮아서(background) OS가
`USE_FULL_SCREEN_INTENT` 실행 자체를 거부한다 → **포그라운드 서비스로 승격시키면 해결될 것**.

### 시도한 수정: `StockAlarmForegroundService` 도입
- `WatchNotificationService.onMessageReceived()`는 데이터만 추출해 새 서비스를 기동만 하도록 변경.
- 신규 `StockAlarmForegroundService`(`android:foregroundServiceType="shortService"`, Android 15+
  전용의 "짧게 실행 후 종료" 타입)가 `ServiceCompat.startForeground(...)`로 즉시 포그라운드
  승격 → 알림 발행 → `stopForeground(STOP_FOREGROUND_DETACH)` + `stopSelf()`로 바로 해제.
- 매니페스트에 `FOREGROUND_SERVICE` 권한과 새 서비스 선언 추가.

컴파일/설치 후 실기기에서 `am start-service`로 서비스 등록 확인, `dumpsys appops`로
`START_FOREGROUND (allow)` 성공 확인 → **포그라운드 승격 자체는 성공**.

### 그런데도 여전히 실패
재테스트 결과:
```
USE_FULL_SCREEN_INTENT (default):
  Reject: [fgsvc-s]2026-07-21 23:26:00.638 (...)   ← 포그라운드 서비스 상태에서도 거부
```
**가설 1 기각**: 프로세스 중요도(background vs foreground-service)는 실제 원인이 아니었다.

---

## 4. 실기기 진단 2차 — logcat에서 진짜 원인 특정: Background Activity Launch(BAL) 차단

`adb logcat`으로 실제 FCM 테스트 알림 수신 시점의 시스템 로그를 직접 확인해 결정적 증거를 찾음.

```
E ActivityTaskManager: Background activity launch blocked! goo.gle/android-bal
  [callingPackage: dev.comon.watch_app; ...
   realCallingPackage: com.samsung.android.wearable.sysui;   ← 삼성 One UI Watch 시스템 UI가
                                                                 우리 PendingIntent를 대신 전송
   balAllowedByPiCreator: BSP.NONE; resultIfPiCreatorAllowsBal: BAL_BLOCK;
   balAllowedByPiSender: BSP.ALLOW_BAL; ...
   balRequireOptInByPendingIntentCreator: true]
```

**진짜 원인**: 화면이 꺼진 상태에서 전체화면 알림을 실제로 "발사"하는 주체가 우리 앱이 아니라
**삼성 One UI Watch의 시스템 UI(`com.samsung.android.wearable.sysui`)** 다. 이 워치는 자체 알림
패널/팝업 로직을 통해 우리가 만든 `PendingIntent`를 대신 전송(relay)한다. Android 15+ 정책상
PendingIntent를 **다른 앱이 대신 전송**할 때는 발신자(sender)뿐 아니라 **PendingIntent를 만든
쪽(creator, 우리 앱)도 명시적으로 백그라운드 액티비티 실행을 허용(opt-in)** 해야 하는데,
우리는 이를 설정하지 않고 있었다(`balAllowedByPiCreator: BSP.NONE`).

### 대응: 불필요해진 포그라운드 서비스 되돌리고, 정확한 fix 적용
- `StockAlarmForegroundService.kt` 삭제, 매니페스트에서 해당 서비스 선언 + `FOREGROUND_SERVICE`
  권한 제거 (실제 원인과 무관했던 복잡도이므로 정리).
- `WatchNotificationService.kt`에서 `PendingIntent.getActivity()` 생성 시
  `ActivityOptions.setPendingIntentCreatorBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)`
  를 담은 `Bundle`을 함께 전달하도록 추가 (API 35+ 가드, `Build.VERSION.SDK_INT >= 35`).

재빌드/재설치 후 재테스트 → 로그 확인:
```
balAllowedByPiCreator: BSP.ALLOW_BAL   ← opt-in 반영 확인됨 (BSP.NONE → BSP.ALLOW_BAL)
resultIfPiCreatorAllowsBal: BAL_BLOCK  ← 그런데도 여전히 최종 결과는 BLOCK
callingUidProcState: CACHED_RECENT     ← 우리 앱 프로세스가 이 시점엔 이미 캐시 상태
```

---

## 5. 최종 결론 — 이 기기(Samsung Galaxy Watch)에 한정된, 코드로 해결 불가능한 벽

Android의 BAL(Background Activity Launch) 정책은 opt-in만으로 충분하지 않고, creator/sender
중 최소 한쪽이 실제로 "보이는 상태(visible)"이거나 시스템 프로세스여야 통과된다. 우리 앱은
이 시점 `CACHED_RECENT`(거의 죽어있는 캐시 상태), 삼성 sysui도 `BOUND_FOREGROUND_SERVICE`일
뿐 visible은 아니라서 둘 다 자격 미달 — 그래서 opt-in을 걸어도 최종적으로 막힌다.

더 근본적으로는, 로그에 함께 찍힌 다음 라인이 핵심이다:

```
I WNoti: [WNotiBlockAppHandler] isBlockedNotification > There is no allowed
  [dev.comon.watch_app] in allowedMobileAppHashMap, have to return
  defaultChannelsConfig block state of watch.
```

삼성 One UI Watch는 AOSP 표준 경로(시스템이 직접 전체화면 인텐트 실행) 대신, **자체 알림 처리
레이어(`WNoti`)가 모든 알림을 가로채 앱을 자체 "허용 앱 해시맵(`allowedMobileAppHashMap`)"에
대조**한다. 우리 앱은 이 목록에 없어 기본(제한적) 정책이 적용되고, 이게 앞서 본 BAL 차단의
실제 트리거다. 이건 표준 Android 권한/AppOps가 아니라 **삼성의 비공개 워치 정책**이며,
공개 문서도 없고(웹 검색으로도 확인 안 됨) 앱 코드로 우회할 방법이 없다.

### 남은 제약
- 이 특정 갤럭시 워치 모델에서는, **앱 코드만으로 화면 꺼짐 상태의 자동 전체화면 팝업을 뚫을
  수 없다.**
- 화면이 켜진 상태에서 알림을 탭해 여는 것은 정상 동작한다(일반 헤즈업 알림 경로는 삼성 레이어를
  거치더라도 사용자 탭에 의한 실행이라 BAL 제한과 무관).
- Pixel Watch 등 비삼성 Wear OS 기기에는 이 삼성 전용 차단이 없으므로, 지금까지의 코드 수정만
  으로 자동 팝업이 정상 동작할 가능성이 높다.

### 확인해볼 수 있는 다음 단계 (코드 밖)
- 워치 자체 설정(`설정 > 앱 > dev.comon.watch_app > 권한/알림`)에 관련 토글이 있는지 확인.
- 페어링된 폰의 Galaxy Wearable 앱에 워치 알림 관련 앱별 설정이 있는지 확인.
- 사이드로드(adb) 디버그 빌드가 아니라 Play/Galaxy Store로 정식 배포된 서명 빌드에서는
  삼성의 허용 목록 등재 여부가 다를 수 있어, 정식 배포본으로 재현되는지 별도 검증 필요.

---

## 6. 최종 코드 변경 요약 (유지된 것)

| 파일 | 변경 내용 |
|---|---|
| `AndroidManifest.xml` | `VIBRATE` 권한 추가 |
| `WatchNotificationService.kt` | 채널/Builder 진동 명시, 채널 ID 버전업(`_v2`), 알림 ID 종목별 고유화, `ActivityOptions.setPendingIntentCreatorBackgroundActivityStartMode`로 BAL creator opt-in |
| `StockAlarmActivity.kt` | `FLAG_KEEP_SCREEN_ON`, `onNewIntent` 처리로 연속 알림 시 최신 정보 갱신 |
| `StockAlarmScreen.kt` | `SwipeToDismissBox`로 스와이프 종료 추가 |

(도입했다가 실제 원인과 무관해 되돌린 것: `StockAlarmForegroundService` 및 관련 매니페스트/권한 —
포그라운드 승격 자체는 성공했으나 `USE_FULL_SCREEN_INTENT` AppOps 거부·BAL 차단 어느 쪽도
해결하지 못해 제거함.)
