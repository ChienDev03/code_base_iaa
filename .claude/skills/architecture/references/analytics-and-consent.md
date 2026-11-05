# Analytics, attribution, and consent

## Event tracking — feature-owned, mock until individually approved

`.ai-kb/features/event_tracking.md` is the approved tracking plan. Only rows explicitly defined there
and requested for implementation may send real events. Every other event remains **mocked**: call
sites are wired, the wire format is not. This keeps guessed event names out of Firebase/Adjust and
makes the real wiring a one-file change per feature. Approved feature events are listed in the tracking catalog; do not assume all trackers are mocks.

- A feature that needs an event owns one `features/<name>/analytics/<Feature>Tracker.kt`: a plain
  class with `@Inject constructor` (no interface, no Hilt module, no scope), one intent-named method
  per event (`trackQuestionAnswered(questionId)`), never a generic `name: String` passthrough.
  Create it with the feature's first event — never an empty tracker.
- For an event without an approved spec, method bodies only log through `AppLog.d` (debug builds):
  `[mock] <event> <param>=<value>`. They
  MUST NOT call `core/analytics/Tracker`, `AttributionTracker`, Firebase, Adjust, or Clarity. Mark
  each with `// TODO(tracking-spec): wire once the official event is defined`.
- Event names and param keys are `private const val` placeholders in the companion, prefixed
  `mock_`. Never invent "real-looking" names or copy them from another project.
- ViewModels, composables, and repositories inject `<Feature>Tracker`; they never import
  `Tracker`/`AttributionTracker` or write an event-name string.
- When an official feature spec is approved — and only then, on the user's request — inject the
  composite `Tracker` into `<Feature>Tracker`, replace only the approved mock bodies, drop their
  `mock_` constants and TODOs, update `.ai-kb`, and log the change. Event strings and parameter keys
  remain private to the feature tracker.

Template: `feature-scaffold/references/templates.md` → `<Name>Tracker.kt`.

**Screen views are not mocked**: `NavigationAnalyticsViewModel` calls `Tracker.screenView`, and the
`CompositeTracker` writes the shared debug log before fan-out. Screens never call `screenView`
themselves. Ad surfaces use the navigation-provided `LocalAdScreenReporter`; full-screen SDK
`Shown` callbacks in `core/ads/AdsTracker` are an explicit exception and report approved ad screen
names through the composite Tracker. See `.ai-kb/features/event_tracking.md`.

**Adjust belongs to PaymentKit**: do not add app-side Adjust calls or revenue events.
`PremiumRepositoryImpl` consumes PaymentKit; it is not an exception to that ownership rule.

## Tracker

`core/analytics/Tracker` (`trackEvent` / `screenView` / `setUserProperty`) is the **only** interface
call sites depend on. `CompositeTracker` fans each call out to every `Tracker` bound `@IntoSet` in
`CoreModule` — currently `FirebaseAnalyticsTracker` and `ClarityTracker`.

`CompositeTracker` is also the single debug-log boundary for real analytics. In debug builds it
uses the Logcat tag `Analytics`: product events are `[event] <name> key=value`, with parameter keys
sorted for stable output, and screen views are `[screen] <screenName>`. It does not log user
properties. Mock feature events do not pass through this boundary and retain their `[mock]` logs.

Never inject a concrete tracker at a call site. Adding a new backend = one more `@IntoSet` binding.

Screen views are reported centrally from `AppNavGraph` via `NavigationAnalyticsViewModel`; screens
do not call `Tracker.screenView` themselves (see `navigation.md`). Ad SDK `Shown` callbacks
are the explicit exception described above.

## Current Clarity initialization

`App.onCreate()` calls `initializeClarity()` directly. It skips an already initialized instance or
an empty configured project ID. `ClarityTracker.screenView` forwards the screen name directly;
`setUserProperty` forwards non-null values as a user ID or custom tag, and generic events are no-ops.
There is no current `ConsentRepository` / `ConsentViewModel` / `AnalyticsSdkConsentGate` flow in this
implementation. Do not assume the former consent-denied guard still exists.

AdsKit's UMP `gatherConsent(activity)` is a separate ad-SDK contract; see the AdsKit feature reference.
Changing consent behavior is product work and requires an explicit decision. This page records
the current implementation without introducing a new runtime policy.

## Attribution (Adjust)

There is no app-side attribution wrapper. The PaymentKit AAR initializes Adjust and reports
subscription revenue itself (`AdjustSettings` in `core/di/PaymentModule`, using
`BuildConfig.ADJUST_APP_TOKEN` / `ADJUST_ENVIRONMENT`; event tokens come from
`assets/adjust_event_tokens.json`). Features never call Adjust.

## Subscription sync lifecycle analytics

`PaywallTriggerViewModel` feeds manual and scheduled successful subscription responses into
`features/premium/analytics/SubscriptionSyncAnalytics`. `SubscriptionAnalyticsStore` persists
account-scoped comparison snapshots and event claims in DataStore, independent of RAM-only
PaymentKit entitlement state. Failed scheduled attempts are excluded before touching the ledger.
`PaymentSessionSync.syncResults` is a buffered Channel-backed Flow for this single consumer so
startup responses survive until navigation subscribes; logout drains old-session queued responses.
Lifecycle deduplication survives restarts, but persistence and external analytics delivery are not
transactional. Detailed classification and regression coverage: `.ai-kb/features/premium.md`.


## Shared logout analytics

`core/analytics/LogoutTracker` is a deliberate cross-cutting exception to feature-owned product
trackers: core session termination must not import Profile. `AuthSessionRepository.logout` accepts
`LogoutEntryPoint` (default Profile), with server-force overriding the value. After local sign-out
succeeds it reports `logout_completed` in NonCancellable context so navigation cannot cancel the
event. Onboarding Name Back supplies ONBOARDING_LOGOUT. `SessionViewModel` reports FORCE_LOGOUT for
an observed authenticated-session expiry redirect; it skips explicit sign-outs via the consumed
user-sign-out marker, anonymous startup and account deletion. Profile-owned events remain in
`ProfileTracker`; exact names/parameters and trigger semantics live in `.ai-kb/features/event_tracking.md`.

## Shared rewarded offers

`core/ads/RewardedOfferTracker` is a shared analytics exception for the Topic rewarded offer
controller reused by Home, Connect and Topic detail, plus the AI chat offer controller. It reports
only approved popup open/explicit close/Upgrade events. SDK start/reward/terminal show failures
remain in `AdsTracker`; background load retries do not produce user-attempt failure events.
Reward-offer Upgrade preserves `topic_ads` / `ai_chat_ads` through the paywall navigation event.
