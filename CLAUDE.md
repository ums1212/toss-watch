# Project Architecture & Coding Guidelines (Android)

This document dictates the architectural rules and code conventions that the AI coding agent must strictly follow when generating or modifying code.

## 0. Build system

Gradle with Kotlin DSL, using a version catalog at `gradle/libs.versions.toml` for all dependency/plugin versions.

Run commands from repo root; use `gradlew.bat` on Windows cmd, `./gradlew` in Git Bash.

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

- `:app`: Top-level entry point. Assembles all feature modules and defines the `Navigation 3` graph.
- `:core:model`: Pure Kotlin module. Contains domain entities, `NetworkResult`, and common DTOs (UI-free).
- `:core:network`: Retrofit setup, common error handlers, and JWT auto-refresh via `OkHttp Authenticator`.
- `:core:datastore`: Secure local storage for session tokens (Access/Refresh JWT). Preferences DataStore persists only ciphertext produced by Tink AEAD (AES256-GCM keyset wrapped by an Android Keystore master key). See `core/datastore/README.md` for the encryption design.
- `:core:database`: Room-backed local cache (e.g. `PortfolioStockCache`) for offline/local data, separate in purpose from `:core:datastore` (which is auth-only).
- `:core:designsystem`: Material 3 shared theme, design tokens, and reusable Atomic components.
- `:core:common`: Global extensions, coroutine dispatcher helpers, logging utilities, and the shared MVI contracts (`UiState`/`UiIntent`/`UiSideEffect`) used by both `:app` features and `:watch-app`.
- `:feature:[auth/dashboard/setting/tosskey]`: Independent business modules enclosing screen UI and UseCases.
- `:watch-app`: Standalone Wear OS application module (own `applicationId`/`namespace` `dev.comon.watch_app`, distinct from the phone app's `dev.comon.toss_watch`). Not a library consumed by `:app` — it is built and installed independently on the watch, and talks to the backend directly (via its own Retrofit/Hilt network setup, gated by an `X-Toss-Watch-Api-Key` header). Internally layered as data/domain/presentation like the feature modules, and reuses `:core:common`'s MVI base classes. Requires `tossWatch.apiBaseUrl` and `tossWatch.watchApiKey` in `local.properties` to build.

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

## 5. Agent Code of Conduct

- Do NOT omit boilerplate wiring, such as Hilt module configurations (@Module, @InstallIn) and @Serializable annotations.

- Presentation layers must NEVER bypass UseCases to access Repositories directly.

- Composable functions exceeding 300 lines must be split into sub-component files immediately.

- `:watch-app` intentionally does NOT depend on `:core:network` or `:core:datastore` — it maintains its own Retrofit/Hilt network stack and local DataStore (see `di/Watch*Module.kt`). Do not "fix" this by wiring it to those modules unless explicitly asked; it is a deliberate separation, not an oversight.