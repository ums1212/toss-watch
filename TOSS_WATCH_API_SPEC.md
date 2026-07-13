# Toss Watch API 명세서

개인 맞춤형 증권 알림 서비스의 백엔드(Django) API 명세.
안드로이드 폰앱(Phase 4)과 Wear OS 워치앱(Phase 5) 클라이언트 개발 시 이 문서를 기준으로 한다.

- **Base URL**: `https://comon.dev` (로컬 개발: `http://127.0.0.1:8000`)
- **인증 방식**: 서비스 전용 JWT (`Authorization: Bearer <access_token>`)
- **Content-Type**: `application/json`
- 모든 시각은 `Asia/Seoul` 기준.

---

## 1. 인증 (Auth)

### 1-1. 구글 소셜 로그인 (회원가입 겸용)

```
POST /api/v1/auth/google/          [인증 불필요]
```

안드로이드 앱이 Google Sign-In으로 받은 `id_token`을 전달하면 서버가 구글 공개키로
최종 검증한 뒤, 신규 유저는 자동 가입시키고 서비스 전용 JWT를 발급한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id_token` | string | ✅ | 구글 Sign-In 결과로 받은 ID Token |

**Response 200**

```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "is_new_user": true,
  "email": "user@gmail.com",
  "has_toss_key": false
}
```

- `access_token` 유효기간: **1시간** / `refresh_token`: **14일**
- `has_toss_key`가 `false`면 클라이언트는 토스 키 등록 화면으로 유도할 것.

**Errors**: `400` id_token 누락 / `401` 구글 토큰 검증 실패·이메일 미검증

### 1-2. Access Token 갱신

```
POST /api/v1/auth/refresh/         [인증 불필요]
```

**Request Body**: `{"refresh": "<refresh_token>"}`

**Response 200**: `{"access": "<새 access_token>"}`

**Errors**: `401` refresh 토큰 만료/위조 → 구글 로그인부터 다시 수행

---

## 2. 유저 설정 (Users)

### 2-1. 토스 API 키 등록/수정

```
POST /api/v1/users/toss-key/       [JWT 필수]
```

유저 본인의 토스증권 Open API 키를 등록한다. `client_secret`은 서버에서
Fernet(AES-128-CBC + HMAC) 양방향 암호화되어 DB에 저장된다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `client_id` | string | ✅ | 토스 발급 클라이언트 ID (`tsck_live_...`) |
| `client_secret` | string | ✅ | 토스 발급 시크릿 (`tssk_live_...`) |

**Response 200**: `{"message": "토스 API 키가 등록되었습니다.", "toss_client_id": "tsck_live_..."}`

**Errors**: `400` 필드 누락 / `401` JWT 없음·만료

### 2-2. 토스 키 등록 상태 조회

```
GET /api/v1/users/toss-key/        [JWT 필수]
```

**Response 200**: `{"has_toss_key": true, "toss_client_id": "tsck_live_..."}`
(시크릿은 어떤 API로도 조회 불가)

---

## 3. 시세 조회 (Ticker)

```
GET /api/v1/toss/ticker/?code=005930    [JWT 필수, 유저당 분당 60회 제한]
```

유저가 등록한 본인 토스 키로 시세를 조회해 워치용 경량 JSON으로 반환한다.
국내(코스피/코스닥) 종목코드와 미국 티커(`AAPL` 등) 모두 지원.

**Query Params**: `code` (필수) — 종목 코드/티커

**Response 200**

```json
{
  "symbol": "005930",
  "name": "삼성전자",
  "price": 294500.0,
  "change_rate": 4.25,
  "currency": "KRW",
  "timestamp": "2026-07-10T13:58:10.000+09:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `name` | string | 종목명 |
| `price` | number | 현재가 |
| `change_rate` | number\|null | 전일 종가 대비율(%), 소수 2자리. 전일 데이터 없으면 null |
| `currency` | string | `KRW` / `USD` |
| `timestamp` | string | 시세 기준시각 (ISO 8601, +09:00) |

**Errors**
- `400` `code` 누락 / 토스 키 미등록 (`{"code": "toss_key_not_registered"}`)
- `401` JWT 없음·만료
- `429` 분당 60회 초과 (Throttled)
- `502` 토스 API 오류 (`{"error": "...", "toss_status": <토스측 HTTP 코드>}`)

---

## 4. 워치 알림 설정 (Notifications)

모든 엔드포인트 **[JWT 필수]**. 본인 소유 알림만 조회/수정 가능 (타인 것은 404).

### 4-1. 알림 목록 조회

```
GET /api/v1/notifications/
```

**Response 200** — 알림 객체 배열 (alarm_time 오름차순)

```json
[
  {
    "id": 1,
    "stock_code": "005930",
    "alarm_time": "09:00:00",
    "is_active": true,
    "watch_fcm_token": "dXNlci10b2tlbg...",
    "disabled_reason": "",
    "created_at": "2026-07-10T14:00:00+09:00",
    "updated_at": "2026-07-10T14:00:00+09:00"
  }
]
```

- `disabled_reason`: 서버가 자동 비활성화한 경우 그 사유
  (예: FCM 토큰 무효, 토스 키 미등록). 유저가 다시 `is_active: true`로 켜면 초기화됨.

### 4-2. 알림 등록

```
POST /api/v1/notifications/
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `stock_code` | string | ✅ | 종목 코드 (예: `005930`, `AAPL`) |
| `alarm_time` | string | ✅ | `HH:MM` — 매일 이 시각(Asia/Seoul)에 발송. 초 단위는 무시됨 |
| `watch_fcm_token` | string | ✅ | 워치 기기의 FCM 등록 토큰 |
| `is_active` | boolean | — | 기본 `true` |

**Response 201** — 생성된 알림 객체 (4-1과 동일 구조)

### 4-3. 알림 단건 조회 / 수정 / 삭제

```
GET    /api/v1/notifications/<id>/
PUT    /api/v1/notifications/<id>/     (전체 수정)
PATCH  /api/v1/notifications/<id>/     (부분 수정 — 워치 FCM 토큰 갱신도 이걸로)
DELETE /api/v1/notifications/<id>/     → 204
```

워치 FCM 토큰이 갱신되면(`onNewToken`) 클라이언트는 해당 유저의 알림들에
`PATCH {"watch_fcm_token": "<새 토큰>"}`을 호출해야 한다.

---

## 5. 워치가 수신하는 FCM 메시지 규격 (Wear OS 클라이언트용)

매분 서버 스케줄러가 `alarm_time`이 일치하는 활성 알림을 골라 발송한다.
**Data-Only 메시지**(notification 필드 없음)이며 **Android priority: high**로 발송되므로,
워치는 Doze 상태에서도 `FirebaseMessagingService.onMessageReceived()`로 수신해
직접 풀스크린 알림을 그려야 한다.

**`RemoteMessage.data` 페이로드** (전부 문자열, 총 4KB 이하 보장)

| 키 | 예시 | 설명 |
|---|---|---|
| `type` | `"stock_alert"` | 메시지 종류 식별자 |
| `stock_code` | `"005930"` | 종목 코드 |
| `stock_name` | `"삼성전자"` | 종목명 |
| `price` | `"72500"` | 현재가 (정수가는 소수점 없음, 예: `"314.95"`) |
| `change_rate` | `"+2.30%"` | 부호 포함 대비율. 계산 불가 시 빈 문자열 |
| `timestamp` | `"1783662000"` | 시세 기준시각 (Unix epoch 초) |

**발송 실패 처리(서버측)**: FCM 토큰이 등록 해제(Unregistered)로 판명되면 해당 알림은
자동으로 `is_active: false` + `disabled_reason` 기록됨 → 폰앱은 알림 목록에서 이를 감지해
유저에게 재설정을 안내할 것.

---

## 6. 서버 운영 참고

| 항목 | 내용 |
|---|---|
| 스케줄러 상주 실행 | `uv run python manage.py run_watch_scheduler` (매분 정각 배치) |
| 배치 1회 수동 실행 | `uv run python manage.py run_watch_scheduler --one-off` |
| FCM 수동 발송 테스트 | `uv run python manage.py test_fcm_send --token=<FCM토큰> [--code --name --price --rate]` |
| 시세 조회 재시도 | 토스 API 일시 장애 시 5초 간격 최대 3회 |
| 필수 환경변수(.env) | `SECRET_KEY`, `TOSS_KEY_ENCRYPTION_KEY`, `TOSS_WATCH_FIREBASE_KEY_PATH`(전용 Firebase 서비스 계정 키), `TOSS_WATCH_GOOGLE_CLIENT_ID` (+ 서버 공용 `TOSS_CLIENT_ID/SECRET`은 개발 검증용) |
| 모니터링 로그 | `[Cron 09:00] 총 X건의 워치 알림 푸시 발송 완료 (성공: Y, 실패: Z)` |
| 토스 API IP 제한 | 서버(고정 IP)가 토스 화이트리스트에 등록되어 있어야 함 |
