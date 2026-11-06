---
name: feature-scaffold
description: Add a feature vertical to this local Android app, including repository, ViewModel, routes, DI, strings and verification.
---

# Scaffold a local feature

Read architecture and, when writing Compose, compose-design-system.
Use features/example for working navigation/UI patterns and features/ledger/data for Room access.

1. Confirm first-load, empty, refresh, action, retry and failure UX before adding a flow.
2. Create data models/repository under features/<name>/data and state/ViewModel/UI under presentation.
   Reuse core/database for shared financial tables. Use DataStore for small settings.
3. Repository operations return DataResult through AppRepository.local. Never introduce server calls.
   Propagate cancellation and use Room transactions for multi-step writes.
4. ViewModels extend AppViewModel<S,E>; immutable state, buffered effects, in-flight action guards.
   No UiState/UiEvent marker interfaces exist in the current base.
5. Feature/generic resources own error text. Shared SnackbarController handles messages.
6. Bind the repository in core/di/RepositoryModule.
7. Define serializable feature NavKeys and its serializers/entries in <Name>Navigation.kt.
   Register the serializer module in navigation/Routes.kt and entries in AppNavGraph.
8. Use hiltViewModel with entry stores, collectAsStateWithLifecycle and lifecycle-aware effects.
9. Keep strings in string_<feature>.xml and use shared design tokens/components.
10. Add a feature tracker only for an actual approved event contract; no speculative telemetry.
11. Update the owning .ai-kb contract and changelog. Compile/build/lint as appropriate.
    Tests/device automation require explicit authorization per AGENTS.md.
12. Offer an optional Maestro draft for a new UI flow.

See references/templates.md for the repository pattern. No new account or cloud-sync flow
belongs in this local base. Domain layers and extra modules require an actual shared use case.
