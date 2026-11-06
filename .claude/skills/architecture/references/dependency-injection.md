# Dependency injection

All modules use Hilt SingletonComponent.

| Module | Responsibility |
| --- | --- |
| CoreModule | TimeProvider -> SystemTimeProvider |
| RepositoryModule | ExampleRepository and LedgerRepository bindings; permitted core-to-feature boundary |
| DatabaseModule | Singleton Room MoneyDatabase, WAL, no destructive migration |
| ServicesModule | AnalyticsProvider, CrashReporter, RemoteSettings, TelemetryControl -> AppServices |

AppServices is singleton and chosen at build time from src/local or src/firebase.
AppSettings and SnackbarController use their singleton injected constructors.
Features depend on repository interfaces; UI does not construct a database or vendor SDK.
