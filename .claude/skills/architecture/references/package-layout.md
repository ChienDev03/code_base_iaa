# Package layout (actual, not aspirational)

This is what currently exists under `com.your.sourbase`. New verticals follow the same boundaries.

```
com.your.sourbase/
├── App.kt                          @HiltAndroidApp application entry
├── MainActivity.kt                 single Activity, @AndroidEntryPoint, hosts AppNavGraph()
│
├── navigation/
│   ├── Routes.kt                   NavKey/ScreenRoute definitions + AppRouteSerializersModule
│   └── AppNavGraph.kt              app Scaffold, NavDisplay host, entries wiring
│
├── core/
│   ├── base/                       AppViewModel, AppRepository, UiState/UiEvent markers
│   ├── config/                     AppConfig (BuildConfig-backed values)
│   ├── di/                         CoreModule, RepositoryModule
│   ├── network/                    ApiResult, AppDispatchers (interceptors/ added when backend lands)
│   ├── datastore/                  AppSettings (DataStore preferences)
│   ├── time/                       TimeProvider (inject instead of Instant.now())
│   ├── logging/                    AppLog (debug-only by default; AppLog.tag("X").d(...))
│   ├── analytics/                  Analytics (mock-first Tracker surface)
│   ├── crash/                      CrashReporter (mock by default)
│   ├── ads/                        AdsManager (mock by default; AAR wiring is opt-in)
│   ├── payment/                    PaymentManager (mock by default; PaymentKit wiring is opt-in)
│   └── ui/                         AppComponents, AppField, SnackbarController
│
├── features/example/              starter feature — copy its shape for new verticals
│   ├── data/
│   │   └── ExampleRepository.kt
│   └── presentation/
│       ├── ExampleScreen.kt
│       └── ExampleViewModel.kt
│
└── ui/theme/                       fixed-light runtime; type/spacing/shape/motion tokens
                                    (Color.kt, Theme.kt, AppSpacing.kt, AppShapes.kt, AppMotion.kt)
                                    No shared size object — component sizes are file-private.
```

## Conditional structure

The tree above is a boundary map, not an exhaustive list. New sub-packages are created only when
a real need appears:

- `core/network/interceptors/` is added together with the first Retrofit module.
- `core/ui/components/` splits out of `core/ui/AppComponents.kt` once two features share a widget.
- `domain/` under a feature is added only when logic is reused across ViewModels or moved out of
  an oversized ViewModel — see **feature-scaffold** skill.
- Optional SDK wrappers (Firebase, PaymentKit, AdsKit, Clarity) remain absent from the dependency
  graph and manifest until their integration is approved per `.ai-kb/README.md`.

Check the owning source directory before creating a package; do not introduce modules speculatively.
