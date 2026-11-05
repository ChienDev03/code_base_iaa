# Package layout (actual, not aspirational)

The root `README.md` contains a larger *proposed* tree. This is what exists.

```
com.lutech.pairpulse/
├── App.kt                          @HiltAndroidApp; initializes channels, foreground syncers, PaymentKit and configured Clarity
├── MainActivity.kt                 single Activity, @AndroidEntryPoint, hosts AppNavGraph()
│
├── navigation/
│   ├── Routes.kt                   NavKey/ScreenRoute definitions + AppRouteSerializersModule
│   ├── AppNavGraph.kt              app Scaffold, NavDisplay host, tab root wiring, PlaceholderScreen
│   ├── NavigationBar.kt            five flat tabs (Home, Connect, AI Coach, Moments, Profile), no FAB
│   ├── SystemNavigationBar.kt      system-bar styling for the shell
│   └── NavigationAnalyticsViewModel.kt   central screen-view tracking (+ AppLog.d "[screen]")
│
├── core/
│   ├── auth/                       AuthApi, session/token infrastructure, device info, temporary pre-login location store
│   ├── base/                       AppViewModel, AppRepository, UiState/UiEvent markers
│   ├── chat/                       reusable chat (Question detail and AI Coach): model/, composer/ (ChatComposerController), ui/ (ChatMessageList, ChatComposer, bubbles); never imports a feature
│   ├── config/                     AppConfig (BuildConfig-backed values)
│   ├── di/                         CoreModule, ApiModule, DataStoreModule, RepositoryModule, TimeModule
│   ├── image/                      AppImageLoader
│   ├── logging/                    AppLog (debug-only by default; AppLog.tag("X").d(...))
│   ├── location/                   shared permission check, one-shot capture, and post-launch foreground sync trigger
│   ├── media/image/crop/           shared placement-based bitmap crop contract, geometry, and cache writer
│   ├── time/                       injectable clocks, ServerClock, retained DailyCycleProvider with a foreground boundary timer
│   ├── util/                       feature-neutral UTF-8 byte counting and Unicode-safe text truncation (Utf8Text.kt)
│   ├── ui/                         components/, modifier/ (pressScale, rememberDebouncedClick), preview/, snackbar/
│   ├── network/                    ApiResult, ApiError(+Envelope), ServerErrorResponse, ApiErrorMapper, factories, interceptors/
│   ├── datastore/                  PreferencesKeys, onboarding/legacy appearance/one-time permission repositories (+ DataStore impls)
│   ├── serialization/              shared kotlinx.serialization Json builder (buildAppJson())
│   ├── analytics/                  Tracker, CompositeTracker, FirebaseAnalyticsTracker, ClarityTracker
│   ├── ads/                        AdsManager (+ AdsKitAdsManager), AdPlacement, AdsConfigFactory, AdsPremiumSyncer, approved AdsTracker — wraps the AdsKit AAR (bound in core/di/AdsModule)
│   ├── payment/                    PaymentManager, RestorePurchasesUseCase, PaymentSessionSync (PaymentKit is provided by core/di/PaymentModule)
│   ├── crashlytics/                CrashReporter, FirebaseCrashReporter
│   ├── remoteconfig/               RemoteConfigManager, FirebaseRemoteConfigManager, RemoteConfigKeys
│   ├── notification/               AppFirebaseMessagingService, NotificationChannels
│   ├── statussharing/              App-wide StatusSharingSource contract; backend implementation pending
│   └── widget/                     WidgetUpdateRequester, WidgetLaunchTarget, WidgetIntentExtras (no feature imports)
│
├── features/splash/
│   ├── data/repository/            AuthRepository, AuthRepositoryImpl (device registration)
│   └── presentation/               Splash contract, ViewModel, screen (routes to Home or Login)
│
├── features/auth/
│   ├── data/                       Auth API/repository and OTP contracts
│   └── presentation/               Google/email OTP login UDF screens and ViewModels
│
├── features/home/
│   ├── analytics/                  HomeTracker (approved events from event_tracking.md)
│   ├── data/model/                 HomeModels.kt (HomeSnapshot, DailyActivity, RecommendedQuestion, JourneyCard…)
│   ├── data/remote/                HomeApi + nullable Home DTOs/domain mapper
│   ├── data/repository/            RemoteHomeRepository, DailyCycleSyncer, HomeSnapshotStore, temporary waiting-question repository
│   └── presentation/               Home UDF contract, ViewModel, LazyColumn UI, components/
│
├── features/connect/
│   ├── analytics/                  ConnectTracker (approved catalog events; mocks for unspecified events)
│   ├── data/{model,remote,repository}/  Connect tabs, topic detail, Question detail API/DTOs/repositories
│   ├── domain/                     AnswerSubmitter (upload voice/image, then submit), QuestionChoices helpers
│   └── presentation/               Connect tabs, topic/ (topic detail), question/ (Question detail)
│
├── features/aicoach/
│   ├── analytics/                  AiCoachTracker (approved catalog events; mocks for unspecified events)
│   ├── data/local/                 Room conversation/message API cache + durable outgoing-message outbox
│   ├── data/model/                 Conversation/message domain models
│   ├── data/remote/                REST DTO/API plus dedicated non-buffering NDJSON send API
│   ├── data/repository/            API-backed repository coordinating remote data and local cache
│   └── presentation/               Landing, history, and chat-detail UDF screens
│
├── features/streak/
│   ├── data/{models,remote,repositories}/  Couple-streak API, DTO mapper, remote repository
│   └── presentation/               Streak UDF screen, month calendar, and Congrats mock UI
│
├── features/widget/
│   ├── data/{model,remote,local,repository}/  Minimal Home/Moments DTOs, scoped snapshots, DataStore cache, avatar files
│   ├── domain/                     UpcomingEventsResolver and independent Mood/Note expiry boundaries
│   ├── glance/{common,streak,events,distance,mood}/  Native layouts, bitmap artwork, timers and source render invalidation
│   ├── update/                     Network/cache-tick workers, shared scheduler, updater and foreground/CRUD syncer
│   └── di/                         WidgetModule (API, cache, requester/cleaner, optional StatusSharingSource set)
│
├── features/settings/
│   └── presentation/               Legacy Settings UDF contract, ViewModel and screen (not the Profile tab)
│
├── features/premium/
│   ├── data/model/                 Entitlement, PremiumPlan
│   ├── data/repository/            PremiumRepository (interface), PremiumRepositoryImpl
│   └── presentation/               PaywallContract, PaywallScreen, PaywallViewModel
│
└── ui/theme/                       fixed-light runtime; retained scheme definitions and type/spacing/shape/motion tokens
                                    (no shared size object — component sizes are file-private, see compose-design-system)
```

## Implemented verticals and conditional structure

The five tabs (Home, Connect, AI Coach, Moments and Profile) are implemented. Other verticals include
onboarding, pairing, notification, streak, daytogether, widget, premium and subscription; see the KB topic
index for current contracts. Profile has its own `features/profile` implementation.

The tree above illustrates important boundaries rather than exhaustively listing every package.
Check the owning source directory before creating a package. There is no global `core/database`;
Connect and AI Coach own their Room caches and dedicated DI modules. `domain/` is conditional,
not exclusive to a single feature. Home uses signed/authenticated `GET api/v1/home`; bundled images
are UI artwork and network-image fallbacks.
