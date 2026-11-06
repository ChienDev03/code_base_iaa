# Actual package layout

Single :app, feature-first UI -> repository architecture.

| Package | Current responsibility |
| --- | --- |
| core/base | AppViewModel, local AppRepository |
| core/data | DataResult, cancellation-safe localOperation |
| core/database | MoneyDatabase, entities, converters, DAO, computed BudgetSummary |
| core/coroutines | AppDispatchers |
| core/config | AppConfig, IntegrationState, RemoteSettings and defaults |
| core/di | CoreModule, RepositoryModule, DatabaseModule, ServicesModule |
| core/datastore | AppSettings |
| core/time | TimeProvider |
| core/analytics, core/crash | Provider interfaces and TelemetryControl |
| core/ads | AdsManager interface, no installed provider |
| core/services | Build-selected AppServices under src/local or src/firebase |
| core/ui, ui/theme | Shared components, snackbar and design tokens |
| features/example | Local UI demo, navigation and ViewModel |
| features/ledger/data | Room-backed LedgerRepository, no screens yet |
| navigation | AppNavGraph and combined route serializer module |

No HTTP transport, account/session or cloud data layer.
Core never imports features except the RepositoryModule composition root.
