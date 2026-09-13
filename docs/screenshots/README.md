# README 스크린샷

2026-09-13 촬영. 이미지는 에뮬레이터가 렌더링한 PNG 원본이며 합성이나 이미지 생성으로 만든 화면이 아닙니다.

| 구분 | 환경 | 데이터 |
| --- | --- | --- |
| 폰 | `emulator-5554`, Android 16 / API 36, 1080 × 2424 | 앱 내장 게스트 계좌·알람 |
| 워치 | `emulator-5556`, Wear OS AVD `Wear_OS_XL_Round`, Android 17 / API 37, 480 × 480 | 캡처용 instrumentation에서 주입한 가상 상태 |

앱 언어는 `ko-KR`입니다. 기기 serial은 환경마다 달라집니다. 워치 원형 디스플레이도 ADB 캡처 파일은 사각형으로 저장됩니다.

## 폰 화면 재현

루트 README의 빌드·설치 절차를 마친 뒤, 로그인 → 게스트로 둘러보기 → 대시보드 → 알림 → 삼성전자 순서로 이동합니다. 로그인 화면은 게스트 진입 전에 촬영합니다.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5554 shell cmd locale set-app-locales dev.comon.toss_watch --user 0 --locales ko-KR
# 촬영할 화면을 연 다음 실행합니다. 파일 이름을 해당 화면에 맞게 바꿉니다.
& $adb -s emulator-5554 shell screencap -p /sdcard/readme-dashboard.png
& $adb -s emulator-5554 pull /sdcard/readme-dashboard.png docs/screenshots/phone-dashboard.png
```

| 파일 | 화면 |
| --- | --- |
| `phone-login.png` | Google 로그인·게스트 체험 |
| `phone-dashboard.png` | 자산 요약·버블 차트 |
| `phone-alarms.png` | 종목별 알람 개수 |
| `phone-detail.png` | 삼성전자 알람 시간·토글·삭제·추가 |

## 워치 화면 재현

[ReadmeScreenshotTest](../../watch-app/src/androidTest/java/dev/comon/watch_app/presentation/ReadmeScreenshotTest.kt)는 실제 앱의 Composable을 에뮬레이터의 Activity에서 실행합니다. 테스트용 상태만 전달하며 서버에 계좌·토큰·알람을 등록하지 않습니다. QR의 `readme-demo-not-a-real-token`은 가짜 값입니다. 주가 알림은 FCM을 보내지 않고 수신 화면 자체를 렌더링합니다.

저장소 루트에서 실행합니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :watch-app:assembleDebug :watch-app:assembleDebugAndroidTest
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5556 install -r watch-app/build/outputs/apk/debug/watch-app-debug.apk
& $adb -s emulator-5556 install -r watch-app/build/outputs/apk/androidTest/debug/watch-app-debug-androidTest.apk
& $adb -s emulator-5556 shell cmd locale set-app-locales dev.comon.toss_watch --user 0 --locales ko-KR
& $adb -s emulator-5556 shell input keyevent KEYCODE_WAKEUP
& $adb -s emulator-5556 shell am instrument -w -e class dev.comon.watch_app.presentation.ReadmeScreenshotTest dev.comon.toss_watch.test/androidx.test.runner.AndroidJUnitRunner

foreach ($name in @('watch-pairing', 'watch-stocks', 'watch-detail', 'watch-notification')) {
    & $adb -s emulator-5556 pull "/sdcard/Android/data/dev.comon.toss_watch/files/readme-$name.png" "docs/screenshots/$name.png"
}
```

QR·종목·상세 목록은 캡처 코드에서 해당 항목으로 스크롤합니다. 주가 알림은 이미지 장면 뒤의 정보 장면을 촬영합니다. 시간 표시는 에뮬레이터 시각에 따라 달라집니다.

문서 작성 시 결과: `OK (4 tests)`. 캡처 파일 8장을 직접 열어 내용을 확인했습니다. 이는 화면 촬영 검증이며 실제 QR 계정 등록, 기기 간 동기화, 예약 시각 FCM 수신은 별도로 검증해야 합니다.
