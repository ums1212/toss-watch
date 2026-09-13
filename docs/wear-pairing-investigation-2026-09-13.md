# Wear QR 페어링 크래시 조사 (2026-09-13)

## 결론

현재 소스(versionCode 7)의 휴대폰 debug 앱에서 심사에 보고된 크래시는 재현되지 않았다.
아래 후보는 정적 코드 검토 결과이며 심사 크래시의 확정 원인이 아니다.
프로덕션 코드를 추정에 따라 수정하지 않았고, 회귀 테스트 및 테스트 도구 호환성 수정만 추가했다.

## 실행한 검증

- Pixel_10 AVD, Android 16 / API 36, x86_64.
- 앱 설치 → 게스트 진입 → 설정 → QR 페어링 → 카메라 권한 허용: 정상.
- `:app:assembleDebug :feature:setting:testDebugUnitTest`: 성공, 설정/ViewModel 테스트 14개 통과.
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.comon.toss_watch.WatchPairingRegressionTest`: 2개 통과.
- 실제 ML Kit에 480px QR bitmap을 입력해 디코딩하고 실제 WatchPairViewModel/WatchPairScreen을 실행했다.
  잘못된 QR은 오류 상태를 표시하고 등록을 호출하지 않는다. 재시도 후 정상 QR은 테스트 저장소에 정확한 필드를 전달하고 화면을 종료한다.
- 카메라 Composable을 10회 제거/재생성하는 테스트 통과. 초기화와 해제 경합의 모든 타이밍을 보장하는 테스트는 아니다.
- 최종 `adb logcat -b crash -d` 출력 없음.

테스트 HTML: `app/build/reports/androidTests/connected/debug/index.html`

## 원인 후보

추가 커밋 이력 조사: `47d3f19`에는 release R8 최적화로 인한
`BarcodeScanning.getClient()` NPE를 방지하는 ML Kit keep 규칙이 들어 있다.
`037c3cc7` 바로 이전 버전에는 이 수정이 이미 포함돼 있으며,
`037c3cc7` 전후 QR 페이로드 및 휴대폰 파싱/등록 코드는 동일하다.
따라서 제출된 휴대폰 APK가 `47d3f19`를 포함하는지 우선 확인해야 한다.
이는 기존 커밋에 기록된 원인이며, 이번 조사에서 해당 구버전 release 크래시를 직접 재현한 것은 아니다.

1. `feature/setting/.../watchpair/component/QrCameraPreview.kt`
   - `cameraProviderFuture.get()`은 예외 처리 밖에 있다. 카메라 provider 초기화 실패는 메인 스레드로 전파될 수 있다.
   - 화면 제거 시 scanner를 닫지만, 아직 끝나지 않은 provider listener를 중지하거나 disposed 상태로 차단하지 않는다.
     listener가 늦게 완료되면 종료한 화면의 카메라를 다시 바인딩하려 시도할 수 있다.
   - `InputImage.fromMediaImage`와 `scanner.process`의 동기 예외에는 프레임을 닫는 경로가 없다.
     비동기 Task 실패 처리는 존재한다. 실제 예외 종류/기기 조건은 로그가 있어야 확인 가능하다.

2. `feature/setting/.../data/repository/SettingRepositoryImpl.kt`
   - 서버 성공 후 `.onSuccess { persistPairedWatch(...) }`는 `safeApiCall`의 try/catch 밖에서 실행된다.
   - 로컬 DataStore 쓰기 등의 예외가 나면 UseCase를 통해 WatchPairViewModel의 `launch`로 전파된다.
     해당 launch에 예외 처리가 없어 등록 직후 앱 종료로 이어질 수 있다.
   - 이번 테스트의 등록 저장소는 fake이므로 실제 서버 응답/디스크 실패는 검증하지 않았다.

## 테스트 도구 수정

기존 Espresso 3.5.1은 API 36에서 테스트 시작 시 `NoSuchMethodException: InputManager.getInstance`로 실패했다.
Espresso 3.7.0으로 변경 후 동일 테스트가 통과했다. 배포 앱의 크래시 원인과 별개인 테스트 의존성 문제다.
공식 수정 내역: https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0

## 검증 범위와 추가 증거

- 실제 워치 화면을 휴대폰 카메라로 촬영한 광학 스캔은 수행하지 않았다. 가상 카메라 실행과 QR bitmap 디코딩을 분리 검증했다.
- 실계정/실서버 등록, Wear 기기의 등록 완료 전환, Play 배포 release/R8 빌드 및 다른 기기/OS는 미검증이다.
- 심사 제출 versionCode, 문제가 발생한 휴대폰 모델/OS, Android vitals 또는 사전 출시 보고서의 원본 스택이 필요하다.
  release 난독화 스택은 제출 버전에 대응하는 mapping으로 확인해야 한다.
- 첨부 심사 문구의 standalone 언급과 별도로 현재 Wear manifest에는 standalone=true가 설정되어 있다.
  앱 온보딩은 휴대폰 QR 등록을 요구한다. 이 선언의 적합성 검토와 휴대폰 크래시 조사는 분리해야 한다.
