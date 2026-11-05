# Networking and core systems

Retrofit 3 + OkHttp 5 + kotlinx.serialization. The converter is
`com.squareup.retrofit2:converter-kotlinx-serialization` — the **first-party Retrofit 3 converter**,
not the older third-party Jake Wharton one. Don't "fix" the coordinate.

Read only the section needed for the task:

| Task | Section |
| --- | --- |
| Results, envelopes, mapping and failure UX | [Failure vocabulary](#failure-vocabulary), [backend envelopes](#backend-response-envelopes), [messages](#apierrormapper-and-user-facing-messages), [classification](#error-classification-and-open-contracts) |
| Request signing, authentication and streaming | [Interceptors](#interceptors), [configuration](#configuration), [streaming](#streaming-responses) |
| Retry and cache behavior | [Repository usage and recovery](#repository-usage-and-recovery) |
| Offline state and blocking UI | [Connectivity](#connectivity-and-offline-ui), [root wiring](#dependency-injection-and-root-status) |
| Device drift, timezone, server clock and rollover gaps | [Device time](#device-time-validation) |
| Shared socket ownership, events and reconnect recovery | [App-wide realtime](#app-wide-realtime-corerealtime) |

## Failure vocabulary

- `ApiResult<T>` exposes `Success(data)`, `Error` and `Loading`. Repositories return this result;
  no `Throwable`, `Response` or `HttpException` crosses into presentation as a failure contract.
- `ApiResult.Error(type, message, serverMessage?, cause?, statusCode?, errorType?, errorCode?)`
  separates client diagnostics (`message`) from backend user copy (`serverMessage`). Equality and
  hashing ignore `cause`. Result helpers include `map`, `onSuccess`, `onError` and `getOrNull`.
- `ApiErrorType`: `Network`, `Timeout`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`,
  `Validation`, `TooManyRequests`, `Server`, `Serialization`, `Unknown`.
- `ApiError` is the legacy compatibility type: `NoConnectivity`, `Timeout`, `Http(code, message)`,
  `Serialization(message)` and `Unknown(message)`. `ApiResult.Error(error)` and `.error` bridge it;
  `ApiError.Http` has no separate `serverMessage` field.
- `AppRepository.safeApiCall` rethrows `CancellationException`; request cancellation is control flow,
  not a user-visible API failure.

## Backend response envelopes

Confirmed from real samples, not speculative.

**Success (HTTP 2xx)**

```json
{ "status": "success", "statusCode": 200, "data": { } }
```

Model as `ApiSuccessEnvelope<T>` and declare it as the Retrofit method return type. `status` and
`statusCode` are nullable with defaults because some successful endpoints omit either metadata field:

```kotlin
suspend fun sync(): ApiSuccessEnvelope<SyncStatusData>
```

The `T` payload model belongs in the **calling feature's** package, never in `core/network`.

**Error (non-2xx)** — observed shapes:

```json
{ "timestamp": "...", "status": false, "statusCode": 401, "error": "Unauthorized",
  "message": "Your session is invalid. Please sign in again.", "errorCode": "AUTH_005" }
{ "timestamp": "...", "statusCode": 404, "error": "Not Found", "message": "V004" }
```

Decoded as `ServerErrorResponse` (`message`, `statusCode`, `error`, `errorType`, `errorCode`,
`details[]{property, code, message}`); `ApiErrorEnvelope` is the older minimal shape. All fields are
nullable because backend error bodies can be incomplete. `error` is an HTTP label ("Unauthorized"),
not user copy. Branch on `errorCode` (`resolvedErrorCode`), never on message text.

Its `status` field is **deliberately not declared**: the backend sends it as a string (`"success"`)
on the success shape but a boolean (`false`) on the error shape, so one shared field would need
inconsistent typing. Do not add it.

## `ApiErrorMapper` and user-facing messages

`ApiErrorMapper.map(throwable: Throwable): ApiResult.Error` (its `.error` property converts to the legacy `ApiError`)

`ServerErrorResponse.resolvedMessage()` produces the **backend message**: every non-blank
`details[].message` joined into one string (newline-separated, de-duplicated); if there are no
details, the non-blank top-level `message`; otherwise `null`. It never falls back to `error`. The
result travels as `serverMessage` on `ApiResult.Error`; `message` holds client diagnostics. The
legacy `.error` adapter stores `serverMessage ?: message` in `ApiError.Http.message`.

Resolution order for any error shown to the user:

1. `serverMessage` (backend text) — always preferred when present.
2. The feature's own string resource for that action (`R.string.<feature>_<action>_error`).
3. The generic / network string resource (generic error, no internet, timeout).

Use `ApiResult.Error.toUiText(@StringRes fallback)` so step 2 is always explicit. Never show
exception text (`throwable.message` / `localizedMessage`), HTTP reason phrases, or the hardcoded
English `ApiErrorMessages` constants to users — log them with `AppLog.d` instead.

### Error-case UX is confirmed with the user

API failures surface as a toast through the app-scoped `SnackbarController` (message per the order
above). What the *screen* shows besides the toast depends on the business case, so **ask the user
before implementing it** and propose options, for example:

| Case | Options to suggest |
| --- | --- |
| First load fails (no data yet) | blank + pull-to-refresh · empty state · error state with Retry |
| Refresh fails | keep existing content (usual) · replace with error state |
| Load-more / paging fails | keep list, toast only (usual) · footer Retry item |
| User action fails (submit, like, …) | toast only · inline/field error · roll back optimistic state |

`ApiResult.Error` is an expected outcome, not a crash: do not send it to `CrashReporter`
(e.g. `recordException(error.cause ?: Exception(error.message))`) unless the user explicitly asks.

## Error classification and open contracts

`ApiErrorMapper` uses `Json` with `ignoreUnknownKeys`, `isLenient` and `coerceInputValues`.
It rethrows `CancellationException`, including a cancellation cause and an `IOException` containing
"canceled". For `HttpException`, it reads the error body with `runCatching`, decodes
`ServerErrorResponse`, maps the HTTP status to `type`/diagnostic `message` and resolves
`serverMessage`. `ApiErrorEnvelope` describes an older shape, not another mapper parser.

| Input | Classification |
| --- | --- |
| HTTP 400/422, 401, 403, 404 | Validation, Unauthorized, Forbidden, NotFound |
| HTTP 408, 409, 429, 5xx | Timeout, Conflict, TooManyRequests, Server |
| `SocketTimeoutException` | Timeout |
| `UnknownHostException` | Network / `NO_INTERNET` |
| Other `IOException` | Network / `CONNECTION_ERROR` |
| `SerializationException` | Serialization |
| Other throwable | Unknown; `localizedMessage` remains diagnostic only |

Error-code precedence is `errorCode`, `errorType`, body `statusCode`, then HTTP code.
`ServerErrorResponse.resolvedErrorCode` calls `ApiErrorCode.fromWireValue(errorCode ?: errorType)`.
The enum matches `wireValue` and then enum name, case-insensitively: `QUOTA_LIMIT_REACHED`,
`EMAIL_OTP_INCORRECT` (wire `EMAIL_OTP_INCORRECT_MESSAGE`), `EMAIL_OTP_EXPIRED`,
`EMAIL_OTP_TOO_MANY_ATTEMPTS`, `EMAIL_OTP_RATE_LIMITED`, `COUPLE_ALREADY_CONNECTED` (wire
`COUPLE_001`) and `UNKNOWN`. Add backend codes when supplied; do not branch on user message text.

Network/timeout failures use network copy. Persistent unauthorized failures follow the auth flow
after `AuthAuthenticator`'s refresh attempt. Validation may map `details[].property` to a field;
rate limits prefer backend copy, with a countdown only when the feature contract specifies it.
Server failures fall back to generic copy. UI helpers include `String?.toErrorUiText()`,
`ApiResult.Error.toUiText(fallback)` and `Throwable.toUiText()`.

Open contracts remain explicit: backend timestamp/signature rejection codes and their accepted
time window are unconfirmed. Do not invent `TIMESTAMP_INVALID`; once agreed, map the code and
trigger `TimeObserver.checkNow()` as appropriate. The mapper still constructs its own `Json`;
reuse a shared serialization instance if that implementation is changed. Existing tests depend
on diagnostic `localizedMessage`; this does not authorize showing it to users.

## Interceptors

`HeaderInterceptor` adds `Accept: application/json` only when an endpoint has not already declared
its own `Accept` value, and always adds a version/flavor-aware `User-Agent`. This preserves
`application/x-ndjson` for streaming endpoints. `AuthInterceptor` adds
the bearer access token (when present) plus device type, language, and time-zone headers.
`SignatureInterceptor` adds a UUID nonce, `ServerClock.nowMillis()/1000` timestamp, and HMAC-SHA256 `sign` query parameter to
every App API request. `AuthAuthenticator` delegates HTTP 401 recovery to `SessionRefresher`, which
serializes refreshes through a separate `@AuthRefreshNetwork` client and retries with the new token.
`AccountDeletionCoordinator` shares that refresh lock through a two-phase guard. Delete Account
enters the navigation guard synchronously in the confirmed button handler, but refresh remains
available while `DELETE users/me` runs so its own HTTP 401 can refresh and retry with the new access
token. After the server confirms deletion, the coordinator blocks refreshes that enter the lock,
while a refresh already holding it must finish response processing and compare-and-set any rotated
token pair before local cleanup continues. This avoids both an unrefreshable DELETE and a
consumed-backend/stale-local token split. The navigation guard stays active through local session
cleanup and signed-out navigation. The DELETE and, after server success, that cleanup run in the
app-scoped `AccountDeletionSession`, not the Delete destination coroutine, so leaving the screen can
neither cancel the request nor strand the guard; the app shell releases the guard only after
observing cleanup completion. A refresh rejected with HTTP 401 or 404 (user no longer exists)
clears the session; other refresh failures are transient and keep it. Body-level HTTP logging is enabled only for debug builds; Chucker is
enabled only for sandbox builds.
`OkHttpClientFactory` configures 15s connect and 30s read/write timeouts.

## Configuration

Missing build configuration can leave `BuildConfig.BASE_URL` empty; `Retrofit.Builder().baseUrl("")`
throws `IllegalArgumentException` when its provider is used. Networked features already exist.
Diagnose this through build/runtime configuration errors; do not inspect dotenv or secret files.

## Repository usage and recovery

Extend `core/base/AppRepository` and use its helpers rather than hand-rolling try/catch:

```kotlin
override suspend fun load(): ApiResult<Snapshot> = safeApiCall { api.fetch().data }
// or, cache-first:
override fun observe(): Flow<ApiResult<Snapshot>> = cacheThenNetwork(cache = dao.observe()) { api.fetch().data }
```

`safeApiCall` defaults to `autoRetryOnRecovery = false`, `maxRetries = 2`: a mutation must not be
repeated automatically. `safeReadCall` opts in for idempotent reads. Each attempt checks coroutine
activity, optionally waits for recovery, runs the block and maps an exception once. Cancellation
is rethrown. With opt-in recovery, network/time failures retry while `attempts <= maxRetries`;
otherwise the mapped error is returned. `isTimeFailure` recognizes `SSLException`, not text
containing "time", "date" or "cert", and combines it with coordinator drift status.

`AppRecoveryCoordinator` treats an absent observer as healthy. `awaitRecoveryIfUnhealthy()` waits
for online/valid time plus 300 ms; `awaitRecovery()` waits for both plus 500 ms. Observers are
registered by `ConnectivityMonitor` and `TimeChecker`, created eagerly through the root status VM.
`safeApiFlow` emits Loading then the result; `cacheThenNetwork` emits an available cache first.
On network recovery, screen reloads apply only to errored, incomplete or empty data; successful
content stays until pull-to-refresh, subject to the owning feature's approved behavior.

## Streaming responses

AI Coach sends messages over `application/x-ndjson`. `AiCoachStreamApi` returns a `ResponseBody`
from a Retrofit method annotated `@Streaming`; the repository layer is responsible for line-by-line
decoding, cancellation, error mapping, and closing the body. It uses the qualified
`@AiCoachStreamingNetwork` client because the shared debug body logger and Chucker can consume the
entire response before the feature sees a chunk. The qualified client retains headers, request
signing, bearer auth and 401 refresh, disables body inspection, and extends the read timeout to 120
seconds.

## Connectivity and offline UI

`ConnectivityObserver` exposes `isOnline: StateFlow<Boolean>`, synchronous `checkNow(): Boolean`
and suspending `awaitOnline()`. Online requires both `NET_CAPABILITY_INTERNET` and
`NET_CAPABILITY_VALIDATED`.

Singleton `ConnectivityMonitor` uses `registerDefaultNetworkCallback`, falling back to
`registerNetworkCallback(NetworkRequest INTERNET)` with `tracksDefaultNetworkOnly = false`.
It tracks validated networks in `ConcurrentHashMap<Network, Boolean>`; online is `values.any { it }`.
With default tracking, `onAvailable` retains only the new network because the platform may omit
the old network's `onLost`; obsolete capability updates are ignored. `onLost` removes a network,
and `onUnavailable` clears state. Initialization registers the observer with the recovery
coordinator. `checkNow()` refreshes from `activeNetwork` capabilities; `awaitOnline()` returns
after a successful check or waits for the flow's first true value.

`MainActivity` refreshes connectivity on resume and window focus, including closing the notification
shade when no resume callback occurs; OEM callback delays must not leave the popup stuck.
`core/ui/components/system/OfflinePopup(visible, modifier, onOpenSettings?)` uses
`BlockingStatusScreen`, a non-dismissible full-screen dialog with the illustration, text and settings
action centered as one group. It blocks back and
debounces its settings action. It opens `Settings.ACTION_WIFI_SETTINGS`, falling back to
`ACTION_SETTINGS`. The button uses `Pink200`/`Pink700`; illustration `image_no_internert_bottom_sheet` is retained. Strings: `common_offline_*`, `action_open_settings` in `string_common.xml`.
Recovery only clears the blocking state; no internet-restored snackbar is emitted.

## Device time validation

### Clock responsibilities

`TimeProvider` / `SystemTimeProvider` reads device time and zone; it is not the server clock.
`UtcDateTime.kt` and `RelativeTimeAgo.kt` own shared parsing/formatting. Device date affects
Streak's today, Home activity/greetings, Day Together anniversaries, profile date limits, invite
expiry countdowns and Room chat cache timestamps. Runtime code uses `TimeProvider`; Streak's
remaining `LocalDate.now()` is a preview. Large drift can also break TLS and token validity.

| Condition | Detection and handling |
| --- | --- |
| Clock drift | Absolute device/server difference over five minutes blocks with `IncorrectTimeDialog` |
| Wrong date | Same drift check; offline uses a same-boot anchor, or the minimum-year fallback if no anchor exists |
| Zone change | `ACTION_TIMEZONE_CHANGED` refreshes checks and the displayed local day; a zone difference alone does not block |

### TimeChecker

`TimeObserver` exposes `isTimeCorrect: StateFlow<Boolean?>` (null before a check), suspending
`checkNow(): Boolean` and `awaitValidTime()`. Singleton `TimeChecker` uses
`CoroutineScope(SupervisorJob() + Dispatchers.IO)`, registers itself with the recovery coordinator,
and listens for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED`.
Polling is every 2,000 ms while incorrect and 60,000 ms while correct/unknown. `awaitValidTime()`
returns when already true or a check succeeds; otherwise it awaits the first true value.

Checks follow this order:

1. Compare against the last server anchor plus monotonic `elapsedRealtime`. SharedPreferences
   persists the anchor, restored only within the same boot.
2. Send an unsigned `HttpURLConnection` HEAD to `BuildConfig.BASE_URL` (same backend clock,
   preferred to google.com), with a three-second timeout and redirects disabled. Read its `Date`;
   update the anchor and compare using the five-minute threshold.
3. Without a Date, retain same-boot anchor drift rather than overwriting it with fallback. With no
   anchor, `SSLException` (including handshake failure) reports false so a future clock that breaks
   certificates is not marked valid. Ordinary unavailable/offline errors fall back to
   `year >= MIN_VALID_YEAR` (2026), avoiding overlapping first-launch offline/time popups.

Do not add another HTTP client where `OkHttpClientFactory` is available. Never log authenticated
URLs. The current unsigned HEAD probe remains the documented `HttpURLConnection` exception.

### ServerClock and rollover gaps

Each successful HEAD syncs `ServerClock` with server time and `SystemClock.elapsedRealtime()`.
Signing uses the wall clock before first sync, then the monotonic server anchor; changing device
time offline does not alter the synchronized signing timestamp. Ordinary responses do not yet
update this anchor. Excessive drift still blocks because TLS/token validity can fail independently.
The backend acceptance window and rejection codes remain unconfirmed.

ViewModels recalculate today when they load; the broadcasts trigger time validation. A shared
flow that automatically updates every open screen at midnight or a time/zone change is not
implemented. A proposed `Flow<LocalDate>` in `core/time` would combine date/time/zone broadcasts
and midnight rollover for Streak, Home and Day Together; do not describe it as shipped.

### Incorrect time UI

`IncorrectTimeDialog(visible, modifier, onOpenSettings?)` uses the same non-dismissible full-screen layout with centered content.
It opens `Settings.ACTION_DATE_SETTINGS`, falling back to `ACTION_SETTINGS`, and closes when time
becomes valid. Strings: `common_incorrect_time_*`, `action_open_settings` in `string_common.xml`.
Its `image_incorrect_device_time` illustration is retained.

## Dependency injection and root status

`core/di/CoreNetworkModule` binds singleton `ConnectivityMonitor` to `ConnectivityObserver` and
`TimeChecker` to `TimeObserver`, and provides `ConnectivityManager`. `TimeModule` separately binds
`TimeProvider` to `SystemTimeProvider`. Singleton `ServerClock` is injected into TimeChecker and
SignatureInterceptor. The manifest includes `ACCESS_NETWORK_STATE`.

`AppContent` eagerly creates `AppStatusViewModel`, initializing both observers and recovery
registration, and calls `onAppActive()` on `ON_RESUME`. `MainActivity` also injects connectivity
for focus checks. `AppContent` collects `AppBlockingStatus` (`None`, `Offline`, `IncorrectTime`)
and renders the corresponding blocking full-screen UI after `AppNavGraph`; offline wins if both conditions
apply, so only one is visible. Healthy state hides it without a recovery snackbar.

## Manual verification coverage

Ask before running unit, instrumented or Maestro tests. Use hand-written fake
`ConnectivityObserver`, `TimeObserver` and `TimeProvider`, not a mocking library or the device clock.
Relevant cases: error details/message/empty/malformed bodies and cancellation; joined detail
messages; offline/time recovery and exhausted retries; five-minute clock threshold and offline
fallback; date-change behavior (the automatic today flow remains proposed). Existing coverage is
not an assertion that the proposed rollover behavior is implemented.

## App-wide realtime (`core/realtime`)
- **Connection ownership**: `RealtimeLifecycleSyncer` (started in `App.onCreate`) connects the socket while an authenticated app is foreground, disconnects 30 s after ON_STOP (grace avoids reconnect churn on quick app switches/dialogs) and immediately on sign-out. While foreground it also watches `ConnectivityObserver.isOnline`: no connect attempt while offline, and regaining network re-runs the idempotent `connect()` (recreates a socket that went `OFFLINE`; a socket still `RECONNECTING` keeps its built-in retries). Feature repositories (chat) never connect/disconnect on their own beyond an idempotent `connect()`; they only join/leave rooms. The server does not replay events missed while disconnected, so `RealtimeLifecycleSyncer` watches `connectionState` through `ReconnectDetector`: every `CONNECTED` that follows an earlier one with a gap (background disconnect, network drop; not the first connect of a session, reset on sign-out) calls `OurTimeTogetherChangeEvents.notifyChanged` and `UserChangeEvents.notifyChanged`, because the resume `couples/me` reload only detects couple id/partner id changes, not anniversary/background or profile edits. Backend's chat doc still says "disconnect when leaving the conversation"; the app deliberately differs because new app-wide events (couple, memory…) ride the same connection.
- **Listeners**: `SocketClient.events(name)` allows several collectors per event name and keeps them across `disconnect()`; they re-attach to the next socket on `connect()`. `connect()` is a no-op unless there is no socket or the state is OFFLINE (then the dead socket is released first).
- **App events**: `RealtimeEventType` enumerates `couple:location_requested|location_updated|pair|paired|unpaired|streak_updated`, `daily_question:answer_submitted`, `topic:answer_submitted`, `memory:created|updated|deleted`, `special_day:created|updated|deleted`, `user:updated`, `subscription:premium_updated`, `our_time_together:updated|background_deleted`, `auth:force_logout`. `RealtimeEventDispatcher` listens to all of them and calls `RealtimeEventHandler` (`DefaultRealtimeEventHandler`, `when` over the enum): `couple:pair` (legacy alias `couple:paired`) → `CoupleChangeSyncer.onRemoteCoupleChanged(CONNECTED)` (no premium sync: the backend follows with `subscription:premium_updated`), `couple:unpaired` → `onRemoteCoupleChanged(DISCONNECTED)` (also runs `PremiumBenefitsSyncer.sync`); `user:updated` → `CoupleChangeSyncer.refreshCoupleInfo` (reloads `couples/me` only, and only while a partner is connected; also `UserChangeEvents.notifyChanged` so Home and About your relationship reload silently); `subscription:premium_updated` (payload `coupleId`, `ownerId`) → when `ownerId` is not `AuthTokenRepository.peekUserId()` (or either is unknown) the handler calls `PremiumBenefitsSyncer.sync` (`PaymentManager.requestSync` = `GET /status`, plus `QuotaRepository.refresh()`; the same call `CoupleInfoSyncer` makes after a disconnect) so the shared premium benefits and quota features stay in sync; ignored when the owner is the current user (their own purchase flow already syncs); the resulting `PaymentManager.isPremium` change makes Home (`onCoupleChanged`) and an open AI Coach chat (`CoachChatViewModel`, existing or newly created — a new chat starts reacting once its conversation id exists —, every already-loaded page 1..N so paged history and the next load-older page are kept, unlocks locked replies; one page failing keeps the current list and only logs; a flip during a send/stream stays pending and runs once the chat is idle, a newer flip replacing it) reload silently; `auth:force_logout` → `AuthSessionRepository.logout(isForceLogout = true)` (skips `authApi.logout()`, clears the session so the app returns to Login, then an error snackbar shows the payload `message`, fallback `auth_force_logout_message`); `our_time_together:updated|background_deleted` → `OurTimeTogetherChangeEvents.notifyChanged` (Home and Our Time Together reload silently from the API; a failed silent reload in `DayTogetherViewModel` only logs and keeps the content — the error snackbar stays for first load, pull-to-refresh and the post-photo-adjust reload); `couple:location_requested` / `couple:location_updated` (both only re-fetch, the app never re-sends its location on these; `location_updated` whose payload `actorUserId` equals `peekUserId()` is ignored because it echoes this device's own `PATCH auth/device/location`, after which Home already reloads) / `couple:streak_updated` / `daily_question:answer_submitted` / `topic:answer_submitted` → `CoupleActivityChangeEvents.notifyChanged(LOCATION|STREAK|ANSWER)`; on `couple:streak_updated` whose payload `completedByUser._id` (`_id`, fallback `id`) equals `AuthTokenRepository.peekUserId()` — or, when the stored session has no user id, differs from the partner id in `CoupleInfoProvider` — (and `streakQualified` is not false; the decision logs id suffixes only) the handler also calls `StreakCelebrationEvents.notifyCompleted` (one per `cycleId`), `CoupleChangesViewModel.streakCelebrations` → `AppNavGraph` pushes `StreakCongratsRoute(cycleId)` on top of the current screen (skipped on signed-out routes or when Congrats is already on top; X pops back). Congrats loads its Mon–today data from the Streak API, and Continue uses the serialized socket cycle id as the Today's conversation alias instead of a mock id. `navigation/CoupleChangesViewModel` exposes `activityReloads` (any change → Home `onCoupleChanged`, silent snapshot reload, which also carries the couple distance and the streak badge), `streakReloads` (→ `StreakViewModel.onRemoteChanged`: silent reload of the shown month, queued behind a running load, failure keeps the content without a toast) and `answerReloads` (→ `ConnectInvalidator.invalidate()` for Connect + Topic detail, and `QuestionDetailViewModel.onRemoteChanged` silent reload), each through its own version in `AppNavGraph` + `ReloadOnCoupleChange`. On a reconnect after a gap `RealtimeLifecycleSyncer` also notifies every `CoupleActivityChange`. Every other type is `Unit` (TODO) until its feature exists. The `couple:streak_updated` event name is assumed (backend spec does not list it yet). Payloads are never logged.
- **Force-logout routing**: `AuthSessionRepository` records the server-forced source before token
  sign-out emits the unauthenticated state. The app returns to Get Started and carries
  `AuthEntryPoint.FORCE_LOGOUT` into Authentication; the user-sign-out marker created by local token
  clearing must not replace it with `LOGOUT`.
- **Auth/disconnect recovery** (`SocketClient`): `connect`/`disconnect`/token recovery run under one `Mutex` (never two sockets), and lifecycle callbacks of a replaced socket are ignored. A `connect_error` whose argument is not a `Throwable` is the server's handshake rejection (a `CONNECT_ERROR` packet; Socket.IO never retries it), so it is always treated as an auth failure whatever its wording; `Throwable` transport errors stay with the manager's own retries unless they mention 401/unauthorized/token. On an auth failure the stale socket is released (stops stale-token retries), `SessionRefresher.refresh` is retried up to 3× on `TransientFailure` (1 s/2 s backoff), then a new socket opens with the new token; up to 3 such recoveries in a row (reset on connect), `Rejected`/`Superseded`/exhaustion → `OFFLINE` (the lifecycle syncer owns sign-out). A server-initiated `io server disconnect` (never auto-reconnected by Socket.IO) retries `socket.connect()` up to 5× with backoff, falling into auth recovery if the token is stale.
- **No replay**: the server does not re-send events missed while disconnected. On every foreground, `CoupleInfoSyncer` silently reloads `GET /couples/me`; `CoupleInfoRepository` publishes only when the snapshot differs, and `navigation/CoupleChangesViewModel` turns a real change (couple id/partner id) into a Home reload, collected at `AppNavGraph` level (hot flow, so it works while Home is not composed) and, when a partner is now paired while Add Partner is on top, signals the Pairing screen (`PairingScreen(partnerPaired)` → `PairingViewModel.onPartnerPairedRemotely`), which shows Connected until Continue. A remote signal cancels obsolete invite-code GET loads and countdowns while keeping any accept POST running. Continue waits for that POST to finish; late accept errors cannot undo confirmed pairing or show a snackbar. Duplicate signals retain the confirmation. Pairing reads and observes the shared `CoupleInfoProvider` partner name from `couples/me` to populate Connected on both devices without an extra request. `COUPLE_ALREADY_CONNECTED` also shows Connected without an error snackbar or automatic pop; when a partner is lost and it was not this user's own disconnect (10 s window): if Home is on top the stack is left untouched (no Add Partner pushed; Home reloads silently), otherwise the stack is reset to `[Home, AddPartner]` (N07). Screen reloads come from `CoupleChangeEvents` (emitted after a local accept/already-connected/disconnect or socket `couple:*` reload, or a resume reload that found a difference): reloads are silent (no spinner): Home/Question detail via `ReloadOnCoupleChange` (applied on return for entries below the top), Connect/Topic detail via `ConnectInvalidator`; Profile follows `CoupleStatusStore`, which every `couples/me` load updates. Unchanged on resume → no UI work; failed reload keeps the previous value. Notifications use their own unread-count refetch + FCM.
