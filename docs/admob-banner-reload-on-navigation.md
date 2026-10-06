# AdMob 배너가 화면 복귀 시 다시 로드되는 문제

루트 화면(`BottomMenuScreen`) 탑바 아래의 AdMob 배너가, 설정·알람 상세 등 다른 화면으로 이동했다가
돌아올 때마다 빈 자리에서 광고를 처음부터 다시 불러오던 문제의 원인과 해결 과정을 정리한다.

- 관련 PR: #21
- 관련 파일
  - `app/src/main/java/dev/comon/toss_watch/navigation/component/AdMobBanner.kt`
  - `app/src/main/java/dev/comon/toss_watch/navigation/TossWatchNavHost.kt`
  - `app/src/main/java/dev/comon/toss_watch/navigation/BottomMenuScreen.kt`

## 1. 증상

| 동작 | 결과 |
| --- | --- |
| 대시보드 ↔ 알림 탭 전환 | 배너가 그대로 유지됨 (정상) |
| 설정 화면 진입 후 뒤로가기 | 배너 자리가 잠깐 비었다가 새 광고가 로드됨 |
| 알람 상세 화면 진입 후 뒤로가기 | 위와 동일 |

보기에 거슬리는 것 외에도, 화면을 오갈 때마다 광고 요청이 새로 나가므로 요청 수 대비 노출률이
떨어진다. AdMob 자체 갱신 주기보다 훨씬 잦은 요청이 된다.

## 2. 원인

### 2.1. `AdView`의 수명이 화면 컴포지션에 묶여 있었다

처음 구현에서는 `AdMobBanner`가 `AdView`를 직접 `remember`하고, 컴포지션에서 빠질 때 `destroy()`했다.

```kotlin
// AdMobBanner.kt (수정 전)
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(/* ... */) {
        // ...
        key(adSize) {
            // (1) AdView가 이 컴포저블의 컴포지션에 묶여 있다.
            val adView = remember {
                AdView(context).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    setAdSize(adSize)
                    loadAd(AdRequest.Builder().build())   // (3) 다시 컴포지션될 때마다 새 요청
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, adView) {
                // ...
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    adView.destroy()                      // (2) 컴포지션에서 빠지면 뷰를 버린다
                }
            }

            AndroidView(factory = { adView }, /* ... */)
        }
    }
}
```

### 2.2. Navigation 3는 최상단이 아닌 엔트리를 컴포지션에서 뺀다

`NavDisplay`는 백스택 최상단 엔트리만 컴포지션에 유지한다(전환 애니메이션 중에만 두 엔트리가 잠깐
함께 있다). `SettingRoute`나 `AlarmDetailRoute`를 push하면 `BottomMenuRoute` 엔트리, 즉
`BottomMenuScreen` 전체가 컴포지션에서 빠진다.

```kotlin
// TossWatchNavHost.kt
NavDisplay(
    backStack = navigator.backStack,
    // ...
    entryProvider = entryProvider {
        entry<BottomMenuRoute> {
            BottomMenuScreen(/* ... */)   // SettingRoute가 위에 쌓이면 이 컴포지션이 통째로 사라진다
        }
        entry<SettingRoute>(metadata = slideOverlayTransitions()) { /* ... */ }
    },
)
```

두 가지가 겹쳐 다음 순서로 문제가 생긴다.

1. 설정 화면으로 이동 → `BottomMenuScreen`이 컴포지션에서 빠짐 → `onDispose`에서 `adView.destroy()`.
2. 뒤로가기 → `BottomMenuScreen`이 새로 컴포지션됨 → `remember` 블록이 다시 실행되어 새 `AdView`
   생성 → `loadAd()` 재호출.
3. 새 광고가 도착할 때까지 배너 자리가 비어 보인다.

### 2.3. 탭 전환과 ViewModel은 왜 괜찮았나

- **탭 전환**: 대시보드/알림 탭은 `BottomMenuScreen` 내부의 로컬 상태(`selectedTab`)로 전환되어
  백스택을 건드리지 않는다. `Scaffold(topBar)` 슬롯이 컴포지션에 그대로 남으므로 배너도 유지된다.
- **ViewModel**: `rememberViewModelStoreNavEntryDecorator()` 덕분에 엔트리별 `ViewModelStore`에 묶여
  있어, 엔트리가 백스택에 남아 있는 한 컴포지션에서 빠져도 살아남는다.

그렇다고 `AdView`를 ViewModel에 넣을 수는 없다. `AdView`는 Activity 컨텍스트를 잡는 `View`라서,
구성 변경을 넘어 살아남는 ViewModel이 들고 있으면 Activity가 누수된다.

## 3. 해결

`AdView`의 소유권을 `NavDisplay` 바깥으로 올린다. 뷰는 Activity의 컴포지션 수명 동안 한 번만 만들고,
`AdMobBanner`는 그 뷰를 화면에 붙였다 떼기만 한다.

### 3.1. `AdView`를 소유하는 홀더 `AdMobBannerState`

```kotlin
// AdMobBanner.kt (수정 후)
@Stable
class AdMobBannerState internal constructor(private val context: Context) {
    private var adView: AdView? = null
    private var adSize: AdSize? = null
    private var isHostResumed = false
    private var isAttached = false

    internal fun obtain(adSize: AdSize): AdView {
        adView?.let { current ->
            if (this.adSize == adSize) return current   // 같은 크기면 로드된 뷰를 재사용
            current.destroy()                           // 크기가 바뀌면 새로 만든다
        }
        return AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(adSize)
            loadAd(AdRequest.Builder().build())
        }.also {
            adView = it
            this.adSize = adSize
        }
    }

    internal fun onAttached() { isAttached = true; updateRefresh() }
    internal fun onDetached() { isAttached = false; updateRefresh() }
    internal fun onHostResumed(resumed: Boolean) { isHostResumed = resumed; updateRefresh() }

    internal fun destroy() {
        adView?.destroy()
        adView = null
        adSize = null
    }

    private fun updateRefresh() {
        val view = adView ?: return
        if (isHostResumed && isAttached) view.resume() else view.pause()
    }
}
```

- `obtain()`은 같은 크기로 이미 만든 뷰가 있으면 그대로 돌려준다. 화면 복귀 시 이 경로를 타므로
  `loadAd()`가 다시 호출되지 않는다.
- 화면 폭이 바뀌면(회전, 창 크기 변경) adaptive 배너 크기가 달라지는데, `AdView`의 광고 크기는 한 번만
  설정할 수 있으므로 이때는 버리고 새로 만든다.
- 광고 자동 갱신은 "Activity가 resumed"이고 "배너가 화면에 붙어 있을" 때만 돌린다. 설정 화면에 가려져
  있는 동안 갱신하면 보이지 않는 노출이 쌓이기 때문이다.

### 3.2. 홀더를 `NavDisplay` 바깥에서 기억한다

```kotlin
// AdMobBanner.kt (수정 후)
@Composable
fun rememberAdMobBannerState(): AdMobBannerState {
    val context = LocalContext.current
    val state = remember(context) { AdMobBannerState(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.onHostResumed(true)
                Lifecycle.Event.ON_PAUSE -> state.onHostResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.destroy()   // Activity의 컴포지션이 끝날 때에만 뷰를 해제한다
        }
    }
    return state
}
```

```kotlin
// TossWatchNavHost.kt (수정 후)
@Composable
fun TossWatchNavHost(/* ... */) {
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()

    // NavDisplay 바깥 — 화면 전환과 무관하게 유지된다.
    val adBannerState = rememberAdMobBannerState()

    // ...
    NavDisplay(
        // ...
        entryProvider = entryProvider {
            entry<BottomMenuRoute> {
                BottomMenuScreen(
                    isGuest = sessionState == SessionState.GUEST,
                    adBannerState = adBannerState,
                    // ...
                )
            }
        },
    )
}
```

`rememberAdMobBannerState()`는 스플래시 분기의 `return`보다 앞에서 호출한다. 그 뒤에 두면 세션 상태가
`LOADING`으로 돌아갈 때 홀더가 컴포지션에서 빠져 뷰가 해제된다.

### 3.3. `AdMobBanner`는 붙였다 떼기만 한다

```kotlin
// AdMobBanner.kt (수정 후)
@Composable
fun AdMobBanner(
    state: AdMobBannerState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(/* ... */) {
        // ...
        val adView = remember(state, adSize) { state.obtain(adSize) }

        DisposableEffect(state, adView) {
            state.onAttached()
            onDispose { state.onDetached() }   // destroy()가 아니라 갱신만 멈춘다
        }

        key(adView) {
            AndroidView(
                factory = {
                    (adView.parent as? ViewGroup)?.removeView(adView)
                    adView
                },
                modifier = Modifier
                    .width(adSize.width.dp)
                    .height(adSize.height.dp),
            )
        }
    }
}
```

주의할 점이 두 가지 있다.

- **이전 부모에서 먼저 떼어내기**: `AndroidView`가 컴포지션에서 빠져도 `AdView`는 이전 `AndroidView`
  컨테이너를 부모로 계속 갖고 있다. 그대로 새 `AndroidView`의 `factory`에서 돌려주면
  `IllegalStateException: The specified child already has a parent`로 크래시한다.
- **`key(adView)`**: `AndroidView`의 `factory`는 노드당 한 번만 호출된다. 크기가 바뀌어 `obtain()`이
  새 뷰를 돌려줬을 때 노드를 새로 만들지 않으면 화면에는 이미 `destroy()`된 옛 뷰가 남는다.

## 4. 검증

- `:app:assembleDebug`, `:app:compileReleaseKotlin` 통과.
- 실기기(SM-S911N, debug 빌드, Google 테스트 광고)에서 설정 화면 진입 → 뒤로가기를 두 번 반복:
  - 복귀 0.5초 시점 스크린샷에 광고가 이미 표시되어 있음(빈 자리 없음).
  - 크래시 없음.
  - logcat `Ads` 태그 기준, 광고 로드 로그는 앱 시작 직후에만 있고 화면 복귀 시점에는 추가되지 않음.

테스트 광고는 크리에이티브 자체가 두 문구("You've loaded a test ad…" / "AdMob Adaptive Banner")를
번갈아 보여 주는 애니메이션이라, 문구가 바뀌는 것만으로는 재로드로 판단하면 안 된다.

## 5. 남은 한계

- **회전/창 크기 변경 시에는 다시 로드된다.** `AdView`의 광고 크기를 바꿀 수 없어 피할 수 없다.
  현재 `MainActivity`는 `configChanges`를 선언하지 않아 회전 시 Activity가 재생성되므로, 이때는 홀더
  자체가 새로 만들어진다.
- **프로세스가 종료됐다 복원되면 다시 로드된다.** 뷰는 저장·복원 대상이 아니다.
- `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`는 `play-services-ads` 25.5.0에서
  deprecated다. 대체 후보인 `getLargeAnchoredAdaptiveBannerAdSize`는 배너 높이가 달라질 수 있어(실측하지
  않음) 화면 구성에 영향을 주므로, 이 수정에서는 바꾸지 않았다.

## 6. 같은 종류의 문제를 피하려면

`NavDisplay` 엔트리 안에서 `remember`한 것은 그 화면이 가려지는 순간 사라진다. 화면 전환을 넘어
유지해야 하는 대상은 종류에 따라 둘 곳이 다르다.

| 대상 | 둘 곳 |
| --- | --- |
| UI 상태(스크롤 위치, 선택 탭 등) | `rememberSaveable` (`rememberSaveableStateHolderNavEntryDecorator`가 복원) |
| 화면 데이터·비즈니스 상태 | 엔트리의 ViewModel |
| 여러 화면이 공유하는 변경 가능한 목록 | 리포지토리의 공유 캐시 (CLAUDE.md §2.1 `:feature:alarm` 참고) |
| Activity 컨텍스트를 잡는 `View` (`AdView`, `WebView` 등) | `NavDisplay` 바깥에서 `remember`한 홀더 — ViewModel에 넣으면 누수 |
