# Dependency injection (Hilt)

All modules live in `core/di/`, all `@InstallIn(SingletonComponent::class)`, split by concern.
Putting a binding in the wrong module is the most common DI mistake here.

| Module | Provides / binds |
| --- | --- |
| `CoreModule.kt` | Firebase Analytics/RemoteConfig/Crashlytics instance providers; tracker/attribution/billing/crash wrappers; `CurrentLocationProvider` → `AndroidCurrentLocationProvider`. |
| `ApiModule.kt` | `Json`, the signed/authenticated shared `OkHttpClient` + `Retrofit`, feature APIs, the qualified refresh-only transport/API, and the qualified non-buffering AI Coach NDJSON transport/API. |
| `DataStoreModule.kt` | Process-wide `DataStore<Preferences>` (via `PreferenceDataStoreFactory.create`, file `pairpulse_preferences`); notification/onboarding repositories and `PendingDeviceLocationStore` → `DataStorePendingDeviceLocationStore`. |
| `RepositoryModule.kt` | Feature repository `@Binds` only, including Home snapshot/location repositories and `StreakRepository` → `RemoteStreakRepository`. **New feature repositories belong here, not in `CoreModule`.** |
| `TimeModule.kt` | `TimeProvider` → `SystemTimeProvider`. |
| `AdsModule.kt` | `AdsManager` → `AdsKitAdsManager` (the only class calling the AdsKit AAR lifecycle API; UI uses `core/ui/components/ads/AppBannerAd`). |

Feature-owned Room databases are provided beside their feature data source because their generated
schema and DAO are not shared infrastructure. `ConnectChatDatabaseModule` provides `connect_chat.db`;
`AiCoachDatabaseModule` provides versioned `ai_coach.db` plus its conversation/message DAOs. Its
conversation-summary and message/outbox store interfaces remain in `RepositoryModule`, the
deliberate feature-importing composition root. AI Coach exposes one coordinated
`SessionDataCleaner`: it invalidates the session generation, cancels and joins in-flight sends, then
clears both stores. Do not bind the two Room stores as independent cleaners because multibinding
iteration order cannot enforce that security boundary.

## Rules

- `core/` must never import a feature; a feature must not import another feature's `data/` or
  `presentation/` internals. (Carried over from root `README.md`.) `RepositoryModule` is the one
  deliberate exception — it is the composition root and imports feature repository types by design.
- Prefer `@Binds` on an abstract module for interface→impl; use `@Provides` only for types you do
  not own (Firebase, OkHttp, Retrofit, DataStore).
- A class with an `@Inject constructor` and `@Singleton` needs **no module entry** — Hilt constructs
  it directly. `SnackbarController` works this way and intentionally has no `CoreModule` binding.
- Adding a new `Tracker` implementation: bind it `@IntoSet` in `CoreModule` and it is automatically
  fanned out by `CompositeTracker`. Do not inject concrete trackers at call sites.
- `@ApplicationContext` is the only `Context` a repository may take.
- Use a qualifier when an integration cannot share transport behavior. `@AiCoachStreamingNetwork`
  keeps the normal backend base URL, signature, auth and authenticator but disables Chucker/body
  logging (which would drain NDJSON before incremental consumption) and uses a 120-second read
  timeout. `@AuthRefreshNetwork` remains the recursion-safe token-refresh transport.
