---
name: architecture
description: Architecture rules for this local-only Android app. Use when moving code, changing repository/results, Room, Hilt, navigation, lifecycle, ads or Firebase boundaries.
---

# Architecture

Single :app, feature-first UDF: UI (Compose/ViewModel) -> Repository -> Room/DataStore.
Add domain only for shared business logic or oversized ViewModels.

## Invariants

1. Core never imports features except core/di/RepositoryModule.
2. Serializable feature NavKeys register in AppRouteSerializersModule.
3. Navigation entries use hiltViewModel and entry-scoped stores.
4. Repository failures use DataResult.Failure; no Throwable, SQL or stored values reach UI.
5. AppViewModel exposes immutable StateFlow and buffered Channel effects. postEvent suspends.
6. One shell SnackbarHostState; collect effects with lifecycle STARTED awareness.
7. Feature repository bindings live in RepositoryModule; infrastructure has concern-specific modules.
8. Hand-rolled test fakes; no mocking library. Test execution follows AGENTS.md authorization.
9. Inject TimeProvider and AppDispatchers. Persist database timestamps in Unix seconds.
10. Product events must be individually approved in .ai-kb/features/event_tracking.md.
11. Errors resolve localized feature -> generic fallback; never exception text.
12. Safe debug logs contain no financial/identity data. No HTTP, account or cloud-sync layer.
13. Preserve existing loading/refresh/action behavior. New UX needs confirmation.
14. UI files <500 lines, ViewModels <1000. Keep feature data and presentation boundaries.
15. Room migrations preserve data. No destructive fallback or main-thread queries.
16. Ads/Firebase service failures never block access to local financial data.
17. Disabled optional SDKs are absent from dependency graphs and manifests.

## Owning references

- references/package-layout.md: current package responsibilities
- references/dependency-injection.md: Hilt module ownership
- references/state-and-messaging.md: actual base class and snackbar contract
- references/networking.md: local results (legacy filename)
- references/navigation.md: route registration and lifecycle
- references/analytics-and-consent.md: optional telemetry and consent
- references/testing.md: meaningful verification and execution boundaries
- .ai-kb/local-storage.md: Room and migration contract
- .ai-kb/firebase.md: build selection, config and collection policy

Related skills: compose-design-system for UI, feature-scaffold for new verticals, navigation-3
for navigation. Keep canonical .claude skills and .agents aliases.
Update owning references and .ai-kb/CHANGELOG.md when architecture changes.
