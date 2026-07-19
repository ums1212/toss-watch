# Toss Watch API 명세서

개인 맞춤형 증권 알림 서비스의 백엔드(Django) API 명세.
안드로이드 폰앱(Phase 4)과 Wear OS 워치앱(Phase 5) 클라이언트 개발 시 이 문서를 기준으로 한다.

- **Base URL**: `https://comon.dev` (로컬 개발: `http://127.0.0.1:8000`)
- **API Prefix**: 모든 엔드포인트는 `/api/v1/toss-watch/` 아래에 마운트된다.
- **인증 방식**: 서비스 전용 JWT (`Authorization: Bearer <access_token>`)
- **Content-Type**: `application/json`
- 모든 시각은 `Asia/Seoul` 기준.

---

## 1. 인증 (Auth)

### 1-1. 구글 소셜 로그인 (회원가입 겸용)

```
POST /api/v1/toss-watch/auth/google/          [인증 불필요]
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
**Errors**
- `400 Bad Request` `id_token` 누락 (`{"error": "body에 'id_token'이 필요합니다."}`)
- `401 Unauthorized` 구글 토큰 검증 실패 (`{"error": "구글 id_token 검증 실패: <상세 사유>"}`) 또는 이메일 미검증/이메일 정보 없음 (`{"error": "검증된 이메일 정보가 없는 토큰입니다."}`)

### 1-2. Access Token 갱신

```
POST /api/v1/toss-watch/auth/refresh/         [인증 불필요]
```

**Request Body**: `{"refresh": "<refresh_token>"}`

**Response 200**: `{"access": "<새 access_token>"}`

**Errors**
- `400 Bad Request` `refresh` 토큰 누락 (`{"refresh": ["This field is required."]}`)
- `401 Unauthorized` `refresh` 토큰 만료/위조 (`{"detail": "Token is invalid or expired", "code": "token_not_valid"}`) → 구글 로그인부터 다시 수행

---

## 2. 유저 설정 (Users)

### 2-1. 토스 API 키 등록/수정

```
POST /api/v1/toss-watch/users/toss-key/       [JWT 필수]
```

유저 본인의 토스증권 Open API 키를 등록한다. `client_secret`은 서버에서
Fernet(AES-128-CBC + HMAC) 양방향 암호화되어 DB에 저장된다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `client_id` | string | ✅ | 토스 발급 클라이언트 ID (`tsck_live_...`) |
| `client_secret` | string | ✅ | 토스 발급 시크릿 (`tssk_live_...`) |

**Response 200**: `{"message": "토스 API 키가 등록되었습니다.", "toss_client_id": "tsck_live_..."}`
**Errors**
- `400 Bad Request` 필드 누락 (`{"error": "body에 'client_id'와 'client_secret'이 모두 필요합니다."}`)
- `401 Unauthorized` JWT 인증 정보 누락/만료/위조 (`{"detail": "Authentication credentials were not provided."}` 혹은 `{"detail": "Token is invalid or expired", "code": "token_not_valid"}`)

### 2-2. 토스 키 등록 상태 조회

```
GET /api/v1/toss-watch/users/toss-key/        [JWT 필수]
```

**Response 200**: `{"has_toss_key": true, "toss_client_id": "tsck_live_..."}`
(시크릿은 어떤 API로도 조회 불가)

### 2-3. 워치 FCM 토큰 등록/갱신

```
PUT /api/v1/toss-watch/users/fcm-token/       [JWT 필수]
```

유저(디바이스) 본인의 워치 FCM 등록 토큰을 저장한다. **유저당 토큰은 1개만 유지**되며,
재등록 시 기존 값을 덮어쓴다. 로그인 직후, 그리고 `onNewToken` 콜백으로 토큰이 갱신될 때마다
호출할 것.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `fcm_token` | string | ✅ | 워치 기기의 FCM 등록 토큰. 비워둘 수 없음 |

**Response 200**: `{"message": "FCM 토큰이 등록되었습니다."}`
**Errors**
- `400 Bad Request` `fcm_token` 누락/빈 값 (`{"error": "body에 'fcm_token'이 필요합니다."}`)
- `401 Unauthorized` JWT 인증 정보 누락/만료/위조

### 2-4. 워치 FCM 토큰 등록 상태 조회

```
GET /api/v1/toss-watch/users/fcm-token/       [JWT 필수]
```

**Response 200**: `{"has_fcm_token": true}`

---

## 3. 계좌/포트폴리오 조회 (Accounts & Portfolio)

### 3-1. 계좌목록 조회

```
GET /api/v1/toss-watch/accounts/     [JWT 필수, 유저당 분당 20회 제한]
```

유저가 등록한 본인 토스 API 키로 토스증권에 개설된 사용자의 계좌 목록을 조회해 반환한다.

**Response 200**

```json
[
  {
    "accountNo": "100012345678",
    "accountSeq": 987654,
    "accountType": "BROKERAGE"
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `accountNo` | string | 계좌번호 |
| `accountSeq` | number | 계좌 식별 키 (주문, 보유 종목 등 기타 토스 API 호출 시 식별자로 사용) |
| `accountType` | string | 계좌 유형 (`BROKERAGE` 등) |

**Errors**
- `400 Bad Request` 토스 키 미등록 (`{"error": "등록된 토스 API 키가 없습니다...", "code": "toss_key_not_registered"}`)
- `401 Unauthorized` JWT 인증 오류
- `429 Too Many Requests` 분당 20회 초과 (`{"detail": "Request was throttled. Expected available in X seconds."}`)
- `502 Bad Gateway` 토스 API 연동 오류 (`{"error": "...", "toss_status": <토스측 HTTP 코드>}`)
- `500 Internal Server Error` 복호화 실패 등 서버 내부 오류 (`{"error": "<상세 에러 메시지>"}`)

### 3-2. 포트폴리오 조회 (Portfolio)

```
GET /api/v1/toss-watch/portfolio/    [JWT 필수, 유저당 분당 20회 제한]
```

유저가 등록한 본인 토스 키로 보유 종목 잔고를 조회해 안드로이드 대시보드용 DTO로 반환한다.

**Query Parameters**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accountSeq` | number |  | 특정 계좌의 식별 키. 지정하지 않을 경우, `GET /api/v1/toss-watch/accounts/`로 조회된 계좌 목록 중 첫 번째 계좌를 기본값으로 사용한다. |

토스 Open API `GET /api/v1/holdings` 응답(계좌당 KRW/USD 종목이 섞일 수 있어
합계는 통화별로 분리됨)을 그대로 가공해 반환한다.

**Response 200**

```json
{
  "summary": {
    "total_investment_krw": 650000,
    "total_investment_usd": 2600000,
    "total_evaluation_krw": 725000,
    "total_evaluation_usd": 3400000,
    "total_profit_loss_krw": 75000,
    "total_profit_loss_usd": 800000,
    "total_return_rate": "26.92"
  },
  "securities": [
    {
      "stock_code": "005930",
      "stock_name": "삼성전자",
      "currency": "KRW",
      "quantity": 10,
      "average_buy_price": 65000,
      "total_buy_amount": 650000,
      "current_price": 72500,
      "total_evaluation_amount": 725000,
      "profit_loss": 75000,
      "return_rate": "11.54"
    },
    {
      "stock_code": "SOXL",
      "stock_name": "Direxion Semiconductor 3X",
      "currency": "USD",
      "quantity": 50,
      "average_buy_price": 52000,
      "total_buy_amount": 2600000,
      "current_price": 68000,
      "total_evaluation_amount": 3400000,
      "profit_loss": 800000,
      "return_rate": "30.77"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `summary.total_investment_krw` | number | 총 매수 금액 중 KRW 종목 합계 |
| `summary.total_investment_usd` | number | 총 매수 금액 중 USD 종목 합계 |
| `summary.total_evaluation_krw` | number | 총 평가 금액 중 KRW 종목 합계 |
| `summary.total_evaluation_usd` | number | 총 평가 금액 중 USD 종목 합계 |
| `summary.total_profit_loss_krw` | number | 총 평가 손익 중 KRW 종목 합계 |
| `summary.total_profit_loss_usd` | number | 총 평가 손익 중 USD 종목 합계 |
| `summary.total_return_rate` | string | 계좌 전체 수익률(%), 소수 2자리 문자열 (토스가 통화 환산해 계산한 단일값) |
| `securities[].stock_code` | string | 종목 코드/티커 |
| `securities[].stock_name` | string | 종목명 (토스 API가 직접 제공) |
| `securities[].currency` | string | 해당 종목 통화 (`KRW` / `USD`) — 아래 금액 필드들은 전부 이 통화 기준 |
| `securities[].quantity` | number | 보유 수량 |
| `securities[].average_buy_price` | number | 평균 매수 단가 |
| `securities[].total_buy_amount` | number | 매수 금액 |
| `securities[].current_price` | number | 현재가 |
| `securities[].total_evaluation_amount` | number | 평가 금액 |
| `securities[].profit_loss` | number | 평가 손익 |
| `securities[].return_rate` | string | 종목별 수익률(%), 소수 2자리 문자열 |

**Errors**
- `400 Bad Request` 토스 키 미등록 (`{"error": "등록된 토스 API 키가 없습니다...", "code": "toss_key_not_registered"}`) 또는 파라미터 타입 오류 (`{"error": "accountSeq 파라미터는 숫자 형식이어야 합니다."}`)
- `401 Unauthorized` JWT 인증 오류
- `429 Too Many Requests` 분당 20회 초과 (`{"detail": "Request was throttled. Expected available in X seconds."}`)
- `502 Bad Gateway` 토스 API 연동 오류 (`{"error": "...", "toss_status": <토스측 HTTP 코드>}`)
- `500 Internal Server Error` 복호화 실패 등 서버 내부 오류 (`{"error": "<상세 에러 메시지>"}`)

---

## 4. 워치 알림 설정 (Notifications)

모든 엔드포인트 **[JWT 필수]**. 본인 소유 알림만 조회/수정 가능 (타인 것은 404).

### 4-1. 알림 목록 조회

```
GET /api/v1/toss-watch/notifications/
```

**Response 200** — 알림 객체 배열 (alarm_time 오름차순)

```json
[
  {
    "id": 1,
    "stock_code": "005930",
    "alarm_time": "09:00:00",
    "is_active": true,
    "disabled_reason": "",
    "created_at": "2026-07-10T14:00:00+09:00",
    "updated_at": "2026-07-10T14:00:00+09:00"
  }
]
```

- `disabled_reason`: 서버가 자동 비활성화한 경우 그 사유
  (예: 워치 FCM 토큰 무효/미등록, 토스 키 미등록). 유저가 다시 `is_active: true`로 켜면(`PUT` or `PATCH`) `disabled_reason`은 빈 문자열(`""`)로 자동 초기화된다.
- 워치 FCM 토큰은 알림 레코드가 아닌 유저(디바이스) 단위로 별도 저장된다 →
  [2-3. 워치 FCM 토큰 등록/갱신](#2-3-워치-fcm-토큰-등록갱신) 참고.

### 4-2. 알림 등록

```
POST /api/v1/toss-watch/notifications/
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `stock_code` | string | ✅ | 종목 코드 (예: `005930`, `AAPL`). 비워둘 수 없음 |
| `alarm_time` | string | ✅ | `HH:MM` — 매일 이 시각(Asia/Seoul)에 발송. 초 단위는 무시(0으로 정규화)됨 |
| `is_active` | boolean | — | 기본 `true` |

워치 FCM 토큰은 이 요청에 포함하지 않는다. 발송 시 서버가 유저 프로필에 등록된 토큰
(`PUT /api/v1/toss-watch/users/fcm-token/`)을 사용하므로, 알림 등록 전에 토큰이 먼저 등록되어 있어야
실제 발송이 이루어진다 (미등록 시 첫 발송 시도에서 자동 비활성화됨 — 5장 참고).

**Response 201** — 생성된 알림 객체 (4-1과 동일 구조)

**Errors**
- `400 Bad Request` 유효성 검사 실패
  - `stock_code` 누락/빈 값: `{"stock_code": ["종목 코드는 비워둘 수 없습니다."]}`
  - `alarm_time` 형식 오류

### 4-3. 알림 단건 조회 / 수정 / 삭제

```
GET    /api/v1/toss-watch/notifications/<id>/
PUT    /api/v1/toss-watch/notifications/<id>/     (전체 수정)
PATCH  /api/v1/toss-watch/notifications/<id>/     (부분 수정)
DELETE /api/v1/toss-watch/notifications/<id>/     → 204
```

워치 FCM 토큰이 갱신되면(`onNewToken`) 클라이언트는 [2-3. 워치 FCM 토큰 등록/갱신](#2-3-워치-fcm-토큰-등록갱신)
(`PUT /api/v1/toss-watch/users/fcm-token/`)을 한 번만 호출하면 된다. 알림 레코드마다 개별 갱신할 필요는 없다.

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

**발송 실패 처리(서버측)**: 아래 경우 해당 알림은 자동으로 `is_active: false` + `disabled_reason`
기록됨 → 폰앱은 알림 목록에서 이를 감지해 유저에게 재설정을 안내할 것.
- 유저 프로필에 워치 FCM 토큰이 아예 등록되어 있지 않음 (`disabled_reason`: `"워치 FCM 토큰 미등록으로 자동 비활성화"`)
- FCM 토큰이 등록 해제(Unregistered)로 판명됨 (`disabled_reason`: `"FCM 토큰 무효로 자동 비활성화: ..."`)

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
