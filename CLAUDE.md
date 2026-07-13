# Project Architecture & Coding Guidelines (Android)

This document dictates the architectural rules and code conventions that the AI coding agent must strictly follow when generating or modifying code.

## 0. Build system

Gradle with Kotlin DSL, using a version catalog at `gradle/libs.versions.toml` for all dependency/plugin versions. Key versions: AGP 9.2.1, Kotlin 2.2.10, compileSdk 37, minSdk 26, targetSdk 37.

Common commands (run from repo root; use `gradlew.bat` on Windows cmd, `./gradlew` in Git Bash):

```
./gradlew assembleDebug              # build debug APK
./gradlew installDebug                # build and install on connected device/emulator
./gradlew test                        # run JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "dev.comon.toss_watch.ExampleUnitTest"   # run a single unit test
./gradlew connectedAndroidTest         # run instrumented tests (app/src/androidTest), needs device/emulator
./gradlew lint                        # run Android Lint
```

## 1. Project Overview & Tech Stack

- **SDK Version:** Min SDK 26 (Android 8.0) or higher.
- **Paradigm:** Multi-module Clean Architecture + MVI Pattern.
- **Core Libraries:**
  - **UI/Design:** Jetpack Compose (Material 3), Compose for Wear OS (for future expansion).
  - **Asynchronous/Streams:** Kotlin Coroutines & Flow.
  - **DI / Navigation:** Hilt / Jetpack Navigation 3 (Type-Safe Routing).
  - **Network / Serialization:** Retrofit 2, OkHttp 3 / Kotlinx Serialization (Moshi/Gson BANNED).
  - **Local Storage / Auth:** Jetpack DataStore + Tink AEAD (Android Keystore-wrapped keyset; EncryptedSharedPreferences BANNED — deprecated) / `Credential Manager` API.

## 2. Multi-Module Architecture & Directory Structure

Enforce strict decoupling between modules. Dependency direction must always flow from external layers inward toward the Domain layer.

### 2.1. Module Specifications

- `:app`: Top-level entry point. Assembles all feature modules and defines the `Navigation 3` graph.
- `:core:model`: Pure Kotlin module. Contains domain entities, `NetworkResult`, and common DTOs (UI-free).
- `:core:network`: Retrofit setup, common error handlers, and JWT auto-refresh via `OkHttp Authenticator`.
- `:core:datastore`: Secure local storage for session tokens (Access/Refresh JWT). Preferences DataStore persists only ciphertext produced by Tink AEAD (AES256-GCM keyset wrapped by an Android Keystore master key). See `core/datastore/README.md` for the encryption design.
- `:core:designsystem`: Material 3 shared theme, design tokens, and reusable Atomic components.
- `:core:common`: Global extensions, coroutine dispatcher helpers, and logging utilities.
- `:feature:[auth/dashboard/setting]`: Independent business modules enclosing screen UI and UseCases.

### 2.2. Feature Module Internal Layering (Feature-Centric)

Instead of a single monolithic data module, encapsulate Domain and Data layers inside each feature module to maximize cohesion.
1. **Domain Layer:** Contains UseCases, Domain Models, and Repository Interfaces (Pure Business Logic).
2. **Data Layer:** Contains Repository Implementations, Remote/Local DataSources, Mappers, and DTOs.
3. **Presentation Layer:** Contains MVI-based ViewModels and Compose UI screens.

## 3. MVI (Model-View-Intent) Specification

Enforce a Unidirectional Data Flow (UDF) with Immutable UI States.
- **UiState / UiIntent / UiSideEffect:** Sealed interfaces for state classes, user actions, and one-time events, respectively.

```kotlin
abstract class BaseMviViewModel<S : E I UiIntent, UiSideEffect UiState,>(initialState: S) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()
    private val _sideEffect = MutableSharedFlow<E>()
    val sideEffect = _sideEffect.asSharedFlow()
    abstract fun handleIntent(intent: I)
    protected fun updateState(reducer: S.() -> S) { _uiState.update { it.reducer() } }
    protected fun sendSideEffect(effect: E) { viewModelScope.launch { _sideEffect.emit(effect) } }
}

## 4. Network Handling & Routing Constraints

- NetworkResult Wrapper Mandatory: All API calls must be wrapped in a NetworkResult sealed interface (Success, ApiError, NetworkError) before reaching the Domain layer.

- OkHttp Authenticator: When receiving a 401 Unauthorized, the Authenticator layer must silently refresh the Access Token using the Refresh Token and retry the failed request automatically.

- Type-Safe Routing: String-based route definitions are BANNED. Use @Serializable objects with Navigation 3.

## 5. Agent Code of Conduct

- Do NOT omit boilerplate wiring, such as Hilt module configurations (@Module, @InstallIn) and @Serializable annotations.

- Presentation layers must NEVER bypass UseCases to access Repositories directly.

- Composable functions exceeding 300 lines must be split into sub-component files immediately.