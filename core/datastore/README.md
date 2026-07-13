# :core:datastore — 세션 토큰 보안 저장소

Access/Refresh JWT를 **Jetpack Preferences DataStore + Tink AEAD** 조합으로 암호화 저장하는 모듈.
(기존 `EncryptedSharedPreferences`는 security-crypto 1.1.0에서 deprecated되어 이 구조로 대체됨)

## 암호화 설계

```
JWT 평문
  → TokenCipher.encrypt()          Tink AEAD, AES256-GCM (+ Associated Data)
  → Base64 인코딩
  → Preferences DataStore 기록      toss_watch_session.preferences_pb (암호문만 저장)
```

키 계층 (envelope encryption):

| 계층 | 위치 | 설명 |
|---|---|---|
| 마스터키 (KEK) | Android Keystore (`android-keystore://toss_watch_master_key`) | 하드웨어 보안 경계 밖으로 절대 나오지 않음. Keystore의 crypto 연산은 플랫폼 프로바이더(Conscrypt/BoringSSL)가 수행 |
| 데이터 암호화 키셋 (DEK) | `toss_watch_keyset_prefs` (SharedPreferences) | Tink `AndroidKeysetManager`가 생성한 AES256-GCM 키셋. **마스터키로 암호화된(wrapped) 상태로만** 디스크에 존재 |
| 토큰 암호문 | `toss_watch_session` (DataStore) | `TokenCipher`를 거친 Base64 암호문만 기록. 평문 토큰은 디스크에 닿지 않음 |

## 구성 요소

- [`TokenStore`](src/main/kotlin/dev/comon/toss_watch/core/datastore/TokenStore.kt) — 외부(`:core:network`)에 노출되는 유일한 계약. 구현체는 `internal`.
- [`DataStoreTokenStore`](src/main/kotlin/dev/comon/toss_watch/core/datastore/DataStoreTokenStore.kt) — DataStore 영속화 + 암복호화 오케스트레이션.
- [`TokenCipher`](src/main/kotlin/dev/comon/toss_watch/core/datastore/crypto/TokenCipher.kt) — Tink `Aead` 래퍼. Associated Data(`toss_watch_session_token`)로 암호문의 용도를 바인딩.
- [`DataStoreModule`](src/main/kotlin/dev/comon/toss_watch/core/datastore/di/DataStoreModule.kt) — Hilt 와이어링: `DataStore<Preferences>`, `Aead`, `TokenStore` 바인딩.

## 설계 결정 사항

- **동기(blocking) 인터페이스 유지** — 소비자가 OkHttp `Interceptor`/`Authenticator`(OkHttp 워커 스레드에서 동기 호출)이므로, `runBlocking` 지점을 이 모듈 내부에 캡슐화했다. suspend 계약으로 바꾸면 블로킹이 네트워크 레이어로 새어 나갈 뿐이다. 메인 스레드에서 호출하지 말 것.
- **복호화 실패 = 세션 없음** — 키 유실(앱 재설치, Keystore 초기화)이나 데이터 변조 시 `null`을 반환해 재로그인 플로우로 유도한다. 크래시하지 않는다.
- **Associated Data 사용** — 동일 키로 암호화된 다른 값이 토큰 자리에 이식되는 공격(ciphertext transplant)을 차단한다.
