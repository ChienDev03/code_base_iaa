# Navigation (Navigation 3)

Chosen over Navigation Compose (Nav2) per the resolved review question in root `README.md`.
For recipes beyond what is here (deep links, multiple back stacks, scenes, conditional navigation),
use the **navigation-3** skill.

## Back stack

`navigation/AppNavGraph.kt`:

```kotlin
val backStack = rememberNavBackStack(
    SavedStateConfiguration { serializersModule = AppRouteSerializersModule },
    LoginRoute,
)
```

- `SplashRoute` is the app start destination. Splash clears the stack and routes to Home, the intro,
  Login, or authenticated onboarding at `OnboardingNameRoute` when the account-scoped JSON draft is
  incomplete.
- The `SavedStateConfiguration`/serializers-module argument is **required**. Each feature owns its
  `@Serializable` `NavKey`s and its own `SerializersModule` (e.g. `ExampleRouteSerializers`) inside
  `features/<name>/presentation/<Name>Navigation.kt`. `navigation/Routes.kt` only composes those
  per-feature modules into the single `AppRouteSerializersModule`
  (`ExampleRouteSerializers + <next feature>RouteSerializers + ...`).
- **Add a new route → add `subclass(XRoute::class, XRoute.serializer())` inside that feature's own
  `SerializersModule`, then chain the feature module into `AppRouteSerializersModule`.** Skipping
  either side compiles and runs fine until the OS kills the backgrounded process; saved-state
  restoration then breaks silently.
- Each feature also exposes one DSL extension — `fun EntryProviderScope<NavKey>.<name>Entries(stack, ...)`
  in the same `<Name>Navigation.kt` — that contains every `entry<XRoute> { ... }` for that feature.
  `AppNavGraph` calls `<name>Entries(...)` once inside `entryProvider { ... }` and never inlines
  `entry<>` blocks itself. Co-locating the route type, the serializer subclass, and the entry block
  removes the three-place wiring rot the old monolithic `AppNavGraph` suffered from.

- `QuestionDetailRoute.entryPoint` is a serialized initial-loading hint (`QuestionDetailEntryPoint`):
  a list item's root `status == "completed"` selects `DISCUSSION`; other/missing statuses select
  `QUESTION`. Connect, Topic detail and Home forward it; `StreakRoute.todaysConversationEntryPoint`
  retains it for the Streak CTA. Explicit notification conversation destinations use `DISCUSSION`.
  Both route arguments default to `QUESTION` for older saved states. The hint only selects a
  skeleton while detail is absent; cached/loaded detail controls the rendered screen afterward.

Navigation is back-stack mutation performed in `AppNavGraph`, not in screens:
`backStack.add(route)`, `backStack.removeLastOrNull()`, or `backStack.clear()` + `add(route)` for a
root replacement. Screens expose `onX: () -> Unit` lambdas and never touch the back stack.

## Rendering and ViewModel stores

```kotlin
NavDisplay(
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        rememberNavigationChromeNavEntryDecorator(contentPadding = …, onSettled = …),
    ),
    transitionSpec = { screenFadeTransform() }, // also pop + predictive pop
    …
)
```

`rememberViewModelStoreNavEntryDecorator()` gives **each back-stack entry its own `ViewModelStore`**.

`AppNavGraph` uses one cross-fade (`screenFadeTransform()`, `AppMotion.ScreenFadeMillis`, in
`navigation/NavigationChrome.kt`) for push, pop and predictive Back on child destinations. Direct
switches between the five bottom-bar destinations use an immediate content transform, avoiding a
220 ms tab-response delay while keeping the standard transition for detail and flow navigation;
the five entries carry explicit top-level metadata because Navigation 3's generated `contentKey` is
not the route itself. Other route entries do not override transition metadata.

`rememberNavigationChromeNavEntryDecorator` (same file) applies the Scaffold padding **per entry key**
(status-bar top inset unless the route owns it, bottom bar height for bar routes) instead of on the
whole `NavDisplay`, so an outgoing screen never re-lays out mid-transition. It also reports the key
once the entry reaches `RESUMED` — NavDisplay only resumes an entry after its transition settles —
and the bottom bar is shown only when both the current and the settled route are bar routes: it
fades in after the destination has finished entering and hides as soon as navigation targets a
route without it.

**Consequence:** a screen's ViewModel must come from
`androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()`. The plain
`androidx.lifecycle.viewmodel.compose.viewModel()` resolves against the wrong store and silently
breaks Hilt injection. `PaywallScreen` was migrated to `hiltViewModel()` specifically because of this.

## Screen-view analytics

Any route that should report to analytics implements `ScreenRoute { val screenName: String }`.
`AppNavGraph` reads the current back-stack entry in `LaunchedEffect(currentRoute)` and calls
`NavigationAnalyticsViewModel.trackScreenView(route.screenName)`, which forwards to the injected
`Tracker`. **Screens never call `Tracker` directly for navigation events.**

Two stateful routes use dynamic screen names. The pre-login `OnboardingRoute` reports its settled
pager page (`feature_01_screen` through `feature_03_screen`), while `LoginRoute` reports its visible
state page (`onboarding_auth_screen`, `enter_email_screen`, or
`enter_verification_code_screen`). `AppNavGraph` skips route-level emission for both and receives
the approved name through a callback. Returning from Enter Email to the existing Authentication
landing page suppresses the otherwise state-driven `onboarding_auth_screen`; a new Login route still
reports its initial landing screen. Feature UI still never imports or calls `Tracker`.

`SplashRoute`, `OnboardingRoute`, and `LoginRoute` carry the approved Authentication entry source
where needed so logout, force-logout, and account-deletion redirects survive intermediate screens
and saved-state restoration. A server `auth:force_logout` clears the session and installs
`OnboardingRoute(FORCE_LOGOUT)`; Get Started forwards that source to `LoginRoute`, while an ordinary
authenticated-session expiry may navigate directly to `LoginRoute(FORCE_LOGOUT)`. Back from the
authenticated Onboarding Name step logs out and replaces the stack with
`LoginRoute(ONBOARDING_LOGOUT)`.

`screenName` is the string that reaches analytics — keep it snake_case and stable.

## App shell

`AppNavGraph` owns the `Scaffold`, the five-tab flat `AppNavigationBar` (Home, Connect, AI Coach,
Moments, Profile — no FAB), and the single snackbar host in `Scaffold.snackbarHost`.

- Routes that paint edge-to-edge and apply `statusBarsPadding()` to their own foreground belong in
  `ownsStatusBarBackground`; the shell then omits its top content padding. Both `StreakRoute` and
  `StreakCongratsRoute` follow this contract, including Congrats loading, loaded, and error states.
- The snackbar host is a **sibling of `NavDisplay`**, so route replacement cannot dispose it, and
  `Scaffold` positions it above the bottom bar.
- The shell (bottom bar) is hidden on `LoginRoute`, `PaywallRoute`, and `AdjustJourneyPhotoRoute`.
- Home remains the root back-stack entry after login. Switching to another top-level tab replaces
  only the entry above Home, and selecting Home removes that entry. This keeps Home's per-entry
  `ViewModelStore`, loaded UI state, and saveable `LazyListState` alive across tab switches while
  still bounding the stack to at most two top-level entries. Home's View all and journey-card
  switches follow the same behavior. Other tabs do not yet retain independent back stacks.
- Successful Login branches on the backend `isNewUser` flag: `false` clears the stack before adding
  `HomeRoute`; `true` pushes `OnboardingNameRoute` above Login. After successful email OTP verification,
  Login retains the Enter Code page and its loading indicator through pending location sync and
  onboarding-draft persistence. The destination callback pushes Onboarding first, then resets the
  hidden Login entry to `Landing`, so no Login page flashes before navigation and a later Back path
  still returns to Login landing rather than the OTP entry screen. After server-side account
  deletion, the destination only signals the Activity-retained `SessionViewModel`; its retained,
  non-cancellable cleanup job clears the authenticated-onboarding draft before local tokens and
  publishes `ReadyForNavigation`. `AppNavGraph` observes that state, replaces the back stack with the
  signed-out intro and then releases the deletion guard. Screen disposal therefore cannot strand the
  old session or refresh lock, and re-registering the deleted identity cannot restore its old
  checkpoint or answers. A valid
  display name then pushes `OnboardingBirthdayRoute(username)`; Birthday Setup Back pops to the name
  step. A valid birthday pushes `OnboardingLivingSituationRoute(username, birthday)`; Living
  Situation Back pops to Birthday, while Next pushes
  `OnboardingAnniversaryRoute(username, birthday, livingSituation)`. Relationship Anniversary Back
  pops to Living Situation; both Next and Skip push
  `OnboardingRelationshipGoalsRoute(username, birthday, livingSituation, anniversary)`, with Skip
  passing a null anniversary. Relationship Goals Back pops to Anniversary; Next pushes
  `OnboardingSmallMomentsRoute(username, birthday, livingSituation, anniversary, relationshipGoals)`.
  Small Moments Back pops to Relationship Goals; Next pushes
  `OnboardingAvatarRoute(username, birthday, livingSituation, anniversary, relationshipGoals)`.
  Choose Avatar Back pops to Small Moments. Selecting a gallery image pushes
  `OnboardingAvatarCropRoute(imageUri)`; closing pops without a result, while Save publishes the
  cached crop path through a `NavigationResultChannel<String>` and pops so Choose Avatar can preview
  it. Choose Avatar Continue pushes `OnboardingCommitmentRoute`; selecting a duration and tapping
  I’m committed pushes `OnboardingAddPartnerRoute`. This onboarding pairing entry explicitly hides
  Back and exposes Skip; global unpaired-couple redirects ignore all authenticated-onboarding routes,
  so an initial `hasPartner=false` signal cannot replace the survey with ordinary `AddPartnerRoute`.
  Add Partner Skip pushes `OnboardingNotificationRoute` while retaining the uncompleted pairing
  destination. Successful pairing instead replaces `OnboardingAddPartnerRoute` with
  `OnboardingNotificationRoute`, so later Back navigation cannot reopen the connected confirmation
  and returns from Notification to Daily Commitment. Daily Commitment consults both the current
  shared couple state and a saveable in-flow completion flag; if pairing is already complete, its
  next action bypasses Add Partner and pushes Notification directly. A confirmed unpair clears that
  in-flow flag. Notification checks current notification access and the
  persisted request history, then advances every completed, skipped, or already-granted outcome to
  `OnboardingLocationRoute`. Location skips its permission UI when device location permission is
  already granted; otherwise it waits for a body tap to request precise/approximate access once,
  enables Next after the result, and opens app settings on later denied interactions. Both the
  already-granted path and Location Next push `OnboardingTailoringRoute`. Its four-step loading
  sequence enables Start journey, which marks onboarding complete, clears the onboarding back stack,
  and opens Home. Notification and Location replace their current route when they auto-skip because
  permission was already granted; Back therefore returns to the previous destination that was
  actually visible. User-completed permission steps remain in the stack and preserve normal Back.
  Every confirmed step persists its answers and next `AuthenticatedOnboardingStep` before mutating
  the back stack. On a later cold start, Splash emits an incomplete draft and `AppNavGraph` always
  restarts at Name. A one-time launch check also replaces any later onboarding route restored directly
  by Navigation 3 after process recreation, while the Activity-retained ViewModel prevents rotation
  from resetting the active flow. The persistence ViewModel exposes the draft to each setup destination
  so locally saved values are prefilled as the user advances again; saving an earlier checkpoint merges
  its confirmed fields without erasing later answers. The intro-carousel completion flag is independent.
- Home opens `StreakRoute` from the daily-streak badge and serializes the optional ISO
  `dailyActivity.streakStart` plus Today's conversation question id. This lets the Streak calendar
  restore its lower bound and route its CTA to the correct daily `QuestionDetailRoute` after process
  recreation.
  Its other unimplemented actions publish the app-scoped informational "Coming soon" snackbar and
  leave the back stack unchanged; shell tab navigation remains independent of them.

Registered routes today: `LoginRoute`, `OnboardingNameRoute`, `OnboardingBirthdayRoute`, `OnboardingLivingSituationRoute`, `OnboardingAnniversaryRoute`, `OnboardingRelationshipGoalsRoute`, `OnboardingSmallMomentsRoute`, `OnboardingAvatarRoute`, `OnboardingAvatarCropRoute`, `OnboardingCommitmentRoute`, `OnboardingAddPartnerRoute`, `OnboardingNotificationRoute`, `OnboardingLocationRoute`, `OnboardingTailoringRoute`, `HomeRoute`, `AdjustJourneyPhotoRoute`,
`ConnectRoute`, `AiCoachRoute`, `MomentsRoute`, `ProfileRoute`, `PaywallRoute` — all implement `ScreenRoute` and all appear in
`AppRouteSerializersModule`. Home, Connect, AI Coach, Moments, Profile and Paywall are implemented
destinations. Settings is legacy; the Profile tab owns a separate feature implementation.

## Dependency risk

`gradle/libs.versions.toml` owns Navigation 3 and lifecycle integration coordinates. Check the
actual resolved dependencies before assuming that an upgrade preserves the APIs used here:

```bash
rtk proxy ./gradlew :app:dependencies --configuration devDebugRuntimeClasspath
```

## Moments analytics origins

`MomentsRoute` is a serializable data class (formerly an object) with a default `MomentsEntryPoint`.
Memory/Special Day detail and Edit Memory serialize the same source; older argument payloads
without it default to `moments`. Home sends the selected journey-card id via HomeUiEvent to
AppNavGraph, which maps it to the approved Home source. NotificationDestinationRoutes maps
NotificationOrigin to push/in-app and Edit Memory inherits its detail source. Bottom navigation
matches Moments by route type. Product opened events wait for the screen's first successful state;
screen-view names retain the central NavigationAnalyticsViewModel pipeline.


## Pairing analytics origins

`AddPartnerRoute` is a serializable data class with `PairingEntryPoint` (default Home for older
object-route payloads). Home, question submit gates (daily/topic), Profile, remote-unpair redirects
and notification routing supply the approved origin. Route checks use the route type so all origins
retain the same navigation chrome and pairing-result behavior. `PairingViewModel` receives the
source through its assisted factory; onboarding passes null and retains its separate plan.
`PairingScreen` waits for STARTED before reporting product opened/Connected opened and routes product
system Back through the existing ViewModel action. The product Back handler is disabled during
onboarding, preserving its hidden header Back button and current system Back policy. The global
remote-unpair helper ignores every `AddPartnerRoute` origin and authenticated-onboarding route.
Widgets and Widget Guide also stay open on remote unpair because the gallery supports unpaired
empty states. Widgets does not observe realtime change signals; a new gallery entry or manual
refresh loads current data.
Central `NavigationAnalyticsViewModel` still owns screen views.

## Push detail back stack

`NotificationDestinationRoutes.toBackStack` builds synthetic parents for cold starts. `NotificationDeepLinkEffect` applies the resolved target through `applyNotificationBackStack`: a push detail appends above the live authenticated stack, omitting its synthetic parent, so Back restores the exact previous entry/tab and ViewModel state. Existing entries must not be cleared/re-added on this path. Root/error fallback targets and in-app notification-list taps retain replacement behavior. Push deduplication compares visible destination identity rather than full route equality: source + canonical daily cycle id (resolved from `DailyQuestionStore`, raw-id fallback) or topic question id, Memory/Special Day resource id, or the Streak/Add Partner route type. Existing route objects are retained even when analytics origins or loading/display hints differ. Destinations below the top are not cleared to; different visible destinations still append. See `.ai-kb/features/notification.md` for resolution/preflight behavior.
`NotificationDestinationRoutes.toBackStack` builds synthetic parents for cold starts. `NotificationDeepLinkEffect` applies the resolved target through `applyNotificationBackStack`: a push detail appends above the live authenticated stack, omitting its synthetic parent, so Back restores the exact previous entry/tab and ViewModel state. Existing entries must not be cleared/re-added on this path. Root/error fallback targets and in-app notification-list taps retain replacement behavior. Push deduplication compares visible destination identity rather than full route equality: question id + source, Memory/Special Day resource id, or the Streak/Add Partner route type. Existing route objects are retained even when analytics origins or loading/display hints differ. Destinations below the top are not cleared to; different visible destinations still append. See `.ai-kb/features/notification.md` for resolution/preflight behavior.

## Widget analytics provenance (2026-11-04)

Launcher deeplinks use NotificationOrigin.WIDGET; PUSH and IN_APP remain notification-only sources.
PendingDeepLinkStore publishes a single immutable PendingDeepLinkRequest (destination + origin).
Consumption claims its identity before navigation, preventing stale async resolution from consuming
newer taps or session-cleared work. Widget detail navigation retains warm stacks like PUSH.
DayTogetherRoute and NewSpecialDayRoute are now serializable data classes with defaulted typed
entry points; their serializer registrations are unchanged. StreakRoute adds a defaulted origin,
and NavigationAnalyticsViewModel delegates streak_opened to the feature-owned StreakTracker.
Existing visible destination identity still ignores analytics source: retained routes keep their
state/source and do not replay opened for a duplicate visible-target tap.
