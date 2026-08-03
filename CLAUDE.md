# Project Architecture & Coding Guidelines (Android)

This document dictates the architectural rules and code conventions that the AI coding agent must strictly follow when generating or modifying code.

## 0. Build system

Gradle with Kotlin DSL, using a version catalog at `gradle/libs.versions.toml` for all dependency/plugin versions.

Run commands from repo root; use `gradlew.bat` on Windows cmd, `./gradlew` in Git Bash.

**JDK:** `JAVA_HOME` on this machine defaults to Android Studio's bundled JBR (`C:\Program Files\Android\Android Studio\jbr`), which is a broken/incomplete install here — invoking `java`/`gradlew` through it fails immediately with `Error: could not open 'C:\Program Files\Android\Android Studio\jbr\lib\jvm.cfg'`. A working JDK 17 is installed at `C:\Program Files\Java\jdk-17`; point `JAVA_HOME` there before running any Gradle command:
- PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"`
- Git Bash: `export JAVA_HOME="C:/Program Files/Java/jdk-17"`

If a build ever fails with the `jvm.cfg` error again, this is the cause — re-point `JAVA_HOME`, don't debug the JBR install itself.

## 1. Project Overview & Tech Stack

- **Paradigm:** Multi-module Clean Architecture + MVI Pattern.
- **Core Libraries:**
  - **UI/Design:** Jetpack Compose (Material 3); Compose for Wear OS in `:watch-app`.
  - **Asynchronous/Streams:** Kotlin Coroutines & Flow.
  - **DI / Navigation:** Hilt / Jetpack Navigation 3 (Type-Safe Routing).
  - **Network / Serialization:** Retrofit 2, OkHttp 3 / Kotlinx Serialization (Moshi/Gson BANNED).
  - **Local Storage / Auth:** Jetpack DataStore + Tink AEAD (Android Keystore-wrapped keyset; EncryptedSharedPreferences BANNED — deprecated) / `Credential Manager` API.
  - **Local Storage / Caching:** Room (`:core:database`) for offline/local data caching (e.g. portfolio stock cache), separate from the DataStore+Tink session-token store.

## 2. Multi-Module Architecture & Directory Structure

Enforce strict decoupling between modules. Dependency direction must always flow from external layers inward toward the Domain layer.

### 2.1. Module Specifications

- `:app`: Top-level entry point. Assembles all feature modules and defines the `Navigation 3` graph. The post-login root destination is `BottomMenuRoute`, rendered by `BottomMenuScreen` (`app/.../navigation/BottomMenuScreen.kt`), which hosts the Dashboard and Alarm screens as two bottom-tab items switched via local Compose state (`selectedTab`) — this switch never touches the `Navigator`/backstack. See §4 for when a screen needs its own `AppRoute` instead of being local-state tab content.
- `:core:model`: Pure Kotlin module. Contains domain entities, `NetworkResult`, and common DTOs (UI-free).
- `:core:network`: Retrofit setup, common error handlers, and JWT auto-refresh via `OkHttp Authenticator`.
- `:core:datastore`: Secure local storage for session tokens (Access/Refresh JWT). Preferences DataStore persists only ciphertext produced by Tink AEAD (AES256-GCM keyset wrapped by an Android Keystore master key). See `core/datastore/README.md` for the encryption design.
- `:core:database`: Room-backed local cache (e.g. `PortfolioStockCache`) for offline/local data, separate in purpose from `:core:datastore` (which is auth-only).
- `:core:designsystem`: Material 3 shared theme, design tokens, and reusable Atomic components.
- `:core:common`: Global extensions, coroutine dispatcher helpers, logging utilities, and the shared MVI contracts (`UiState`/`UiIntent`/`UiSideEffect`) used by both `:app` features and `:watch-app`.
- `:feature:[alarm/auth/dashboard/setting/tosskey]`: Independent business modules enclosing screen UI and UseCases.
  - `:feature:alarm`: Owns `AlarmProfile`, alarm CRUD UseCases, and both the alarm-list screen (`AlarmScreen`) and per-stock detail screen (`AlarmDetailScreen`). `AlarmRepository` holds the fetched alarm list in a single in-memory `MutableStateFlow` cache (not per-ViewModel state) — every mutation (add/toggle/delete) updates that cache on success, and both screens observe it via `ObserveAlarmProfilesUseCase`. This is why toggling/deleting in `AlarmDetailScreen` is reflected in `AlarmScreen`'s per-stock alarm counts immediately, with no refetch on navigating back. Follow this shared-cache-in-the-repository pattern for any future case where two screens need to see the same mutable list stay in sync without a shared ViewModel.
- `:watch-app`: Standalone Wear OS application module with its own `namespace` (`dev.comon.watch_app`) but the **same `applicationId`** as the phone app (`dev.comon.toss_watch`) — intentional, so both ship as form factors of a single Google Play app listing. Since Play Console requires one signing key per `applicationId`, `:watch-app` release builds must be signed with the same keystore/signing key as `:app`, not a separate one. Not a library consumed by `:app` — it is built and installed independently on the watch, and talks to the backend directly (via its own Retrofit/Hilt network setup, gated by an `X-Toss-Watch-Api-Key` header). Internally layered as data/domain/presentation like the feature modules, and reuses `:core:common`'s MVI base classes. Requires `tossWatch.apiBaseUrl` and `tossWatch.watchApiKey` in `local.properties` to build.

### 2.2. Feature Module Internal Layering (Feature-Centric)

Instead of a single monolithic data module, encapsulate Domain and Data layers inside each feature module to maximize cohesion.
1. **Domain Layer:** Contains UseCases, Domain Models, and Repository Interfaces (Pure Business Logic).
2. **Data Layer:** Contains Repository Implementations, Remote/Local DataSources, Mappers, and DTOs.
3. **Presentation Layer:** Contains MVI-based ViewModels and Compose UI screens.

## 3. MVI (Model-View-Intent) Specification

Enforce a Unidirectional Data Flow (UDF) with Immutable UI States.
- **UiState / UiIntent / UiSideEffect:** Sealed interfaces for state classes, user actions, and one-time events, respectively.

## 4. Network Handling & Routing Constraints

- NetworkResult Wrapper Mandatory: All API calls must be wrapped in a NetworkResult sealed interface (Success, ApiError, NetworkError) before reaching the Domain layer.

- OkHttp Authenticator: When receiving a 401 Unauthorized, the Authenticator layer must silently refresh the Access Token using the Refresh Token and retry the failed request automatically.

- Type-Safe Routing: String-based route definitions are BANNED. Use @Serializable objects with Navigation 3.

- Route vs. local-state tab content: give a screen its own `AppRoute` only if it must be pushed onto/popped off the `Navigator` backstack — i.e. it needs system back button/predictive-back support and/or a full-screen overlay transition (`slideOverlayTransitions()` in `TossWatchNavHost.kt`). Screens that are just alternate views within the same parent destination (e.g. the Dashboard/Alarm bottom tabs inside `BottomMenuScreen`) should switch via local Compose state instead — do not invent an `AppRoute` for a tab.

## 5. Agent Code of Conduct

- Do NOT omit boilerplate wiring, such as Hilt module configurations (@Module, @InstallIn) and @Serializable annotations.

- Presentation layers must NEVER bypass UseCases to access Repositories directly.

- Composable functions exceeding 300 lines must be split into sub-component files immediately.

- `:watch-app` intentionally does NOT depend on `:core:network` or `:core:datastore` — it maintains its own Retrofit/Hilt network stack and local DataStore (see `di/Watch*Module.kt`). Do not "fix" this by wiring it to those modules unless explicitly asked; it is a deliberate separation, not an oversight.

- When a screen with its own `Scaffold` is hosted as tab content inside another `Scaffold` (e.g. `DashboardScreen`/`AlarmScreen` inside `BottomMenuScreen`'s `bottomBar`), set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` on the inner `Scaffold`. Each `Scaffold` independently reserves space for the system bottom inset by default (`ScaffoldDefaults.contentWindowInsets`); leaving the inner one at its default double-reserves that inset on top of the outer `bottomBar`'s own height, producing a visible empty gap above the bottom bar.

- Guest mode (게스트 체험 모드): `:core:datastore`'s `GuestModeStore` tracks whether the current session is a dummy-data guest session (e.g. for Google Play review to browse without a real login) — implemented as a separate flag rather than a fake token, since a fake token would flow through `AuthInterceptor`/`TokenAuthenticator` and get wiped on the first real 401. Any feature module that talks to the backend and needs guest support (`:feature:alarm`, `:feature:dashboard`, `:feature:setting`, `:feature:tosskey` today) follows the same shape: a `remote` repository, a `guest` repository (in-memory dummy data), and an internal `*RepositoryRouter` implementing the domain `Repository` interface that delegates to whichever is active based on `GuestModeStore.isGuestMode()`/`observeGuestMode()`. Because Hilt `@Binds` fixes the bound implementation at compile time, these routers are wired with `@Provides` in the feature's `*DataModule` (not `@Binds`) so the active implementation can flip at runtime. Follow this Router pattern — not an `if (isGuestMode)` branch inside a single repository impl — for any future feature module that needs guest-mode support.