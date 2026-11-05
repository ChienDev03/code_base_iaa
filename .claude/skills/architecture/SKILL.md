---
name: architecture
description: The architecture rules and non-obvious invariants of this Android app (com.your.sourbase) — read before adding, moving, or refactoring anything outside a single file's body. Use when a task touches Navigation 3 routes or the back stack, Hilt/DI module placement, the core/ vs features/ boundary, core/network and the backend response envelopes, core/base (AppViewModel/AppRepository), one-shot events and the app-scoped snackbar, analytics/attribution/consent gating, or the testing conventions; also when deciding where a new file belongs, or answering "how does X work here". Replaces the former .ai-kb/architecture.md.
---

# Architecture

Feature-first, single Gradle module (`:app`), Google UDF layers: **UI (Compose + ViewModel) → Data
(Repository)**. A `domain/` layer is added only when logic is reused across ViewModels or must be
pulled out of an oversized ViewModel; do not create one speculatively. The current app uses one
Gradle module and feature-first boundaries. The package-layout reference owns the structural map;
do not introduce modules speculatively.

## Invariants — never violate these

1. **`core/` must never import a feature.** A feature must not import another feature's `data/` or
   `presentation/` internals. Cross-feature sharing goes through `core/`.
2. **Every route must be registered in `AppRouteSerializersModule`** (`navigation/Routes.kt`).
   Forgetting this compiles fine and breaks saved-state restore silently.
3. **Screens use `hiltViewModel()`, never `viewModel()`** — Navigation 3's per-entry
   `ViewModelStore` makes the plain factory resolve against the wrong store and silently break
   Hilt injection.
4. **`ApiError` is the only failure type a repository exposes to a ViewModel.** No `Throwable`,
   `Response`, `HttpException`, or Retrofit type crosses into presentation.
5. **One-shot events and user messages use buffered `Channel`s**, never `MutableSharedFlow(replay = 0)`.
   `StateFlow`/`SharedFlow` remain correct for multicast repository state.
6. **No screen owns a `SnackbarHostState`.** One host, in the app `Scaffold` in `AppNavGraph`.
7. **Feature repository `@Binds` go in `core/di/RepositoryModule`**, never in `CoreModule`.
8. **No mocking library.** Every test uses a hand-rolled fake implementing the real interface.
9. **Inject `TimeProvider`** (`core/time`) instead of calling `Instant.now()` / `LocalDate.now()`.
10. **Event tracking is feature-owned and mock-first**: features call
    `features/<name>/analytics/<Feature>Tracker`, never `Tracker.trackEvent` / Adjust directly from
    ViewModels or screens. A feature tracker may inject `Tracker` only for event names and parameters
    explicitly approved in `.ai-kb/features/event_tracking.md`; all unspecified events remain
    `AppLog.d("[mock] …")`. See `references/analytics-and-consent.md`.
11. **User-facing errors: backend message first** (joined `details[].message`, else `message`),
    then the feature's string resource, then the generic one. Never exception text. See
    `references/networking.md`.
12. **Complex business functions log with `AppLog.d`** (debug-only) — action entry, result, error
    branch; small UI functions (stateless composables, formatters, trivial mappers) may skip it.
    Never tokens, bodies, or personal data.
13. **Don't assume every file follows the base classes** — legacy features may predate `core/base`
    and be intentionally left un-migrated. Read the file before assuming the pattern.
14. **File size limits** — UI files < 500 lines, ViewModels < 1000 lines of logic. Split UI into
    sibling section files; move ViewModel logic into repositories, use cases (`domain/`), or helpers.

15. **Date/time formatting is shared** — use `core/time` (`Instant.timeAgo`, `Instant.format`) and
    `core/ui/format` (`timeAgoText`); no per-feature formatters or duplicated "x ago" strings.

## Where things live

`references/package-layout.md` — the actual package tree (not the aspirational one in `README.md`),
what each `core/` package holds, and which structures are conditional.

Deciding where a new file goes:

| What you are adding | Where |
| --- | --- |
| Screen / ViewModel / UI state | `features/<name>/presentation/` |
| Data model, repository | `features/<name>/data/{model,repository}/` |
| Composable used by 2+ features | `core/ui/components/` (else `features/<name>/presentation/components/`) |
| Design token | `ui/theme/` — and only there |
| Retrofit API interface | `features/<name>/data/` (payload models too, never `core/network`) |
| Feature repository binding | `core/di/RepositoryModule.kt` |
| SDK wrapper / infrastructure `@Provides` | `core/di/CoreModule.kt` (or `ApiModule`/`DataStoreModule`/`TimeModule` by concern) |
| Interceptor | `core/network/interceptors/` |
| Route | `navigation/Routes.kt` + `AppRouteSerializersModule` + `AppNavGraph` entry |

## Topic references

Read the one that matches your task — each carries the gotchas that are not visible from the code:

| File | Covers |
| --- | --- |
| `references/package-layout.md` | Actual package tree; what is deferred and why |
| `references/navigation.md` | Navigation 3 back stack, serializers module, per-entry ViewModelStore, screen-view analytics, app shell / five tabs, alpha-coordinate risk |
| `references/dependency-injection.md` | The four-plus Hilt modules and which binding belongs where; multibound `Tracker` set |
| `references/networking.md` | Results/errors, envelopes, auth/signing, recovery, connectivity/offline UI, device/server time and app-wide realtime; use its section index for focused reads |
| `references/state-and-messaging.md` | `AppViewModel`/`AppRepository`, lifecycle-aware Channel collection, and the app-scoped snackbar pipeline |
| `references/analytics-and-consent.md` | Approved-event/mock fallback rule, `Tracker`/`CompositeTracker`, PaymentKit-owned Adjust, current Clarity initialization |
| `references/testing.md` | Tests run only after user confirmation; Maestro proposal; hand-rolled fakes, coroutine-test helpers, DataStore in plain JVM tests, hot-flow race avoidance, accepted coverage gaps |

## Related skills and docs

- **compose-design-system** skill — everything under `core/ui` and `ui/theme`: tokens, component
  catalog, the Material 3 boundary, interaction contract. Architecture-level note: `core/base` is
  deliberately separate from `core/ui` — `AppViewModel`/`AppRepository` are UDF/data base classes,
  not UI component base classes. Text-bearing surfaces use `heightIn(min = …)` rather than fixed
  heights so font scaling works.
- **feature-scaffold** skill — the step-by-step for adding a vertical, with file templates.
- **navigation-3** skill — deep links, multiple back stacks, scenes, conditional navigation.
- `.ai-kb/` — feature-level docs (`features/*.md`), `design-system.md`, `release-delivery.md`.
  Architecture lives here in this skill, not in `.ai-kb`.

## Keeping this current

When a change alters an architecture pattern — a new DI module, a new base type, a new navigation
shape, a new `core/` package, a changed envelope — update the matching file under `references/` and
the invariant list above in the same task, then log it in `.ai-kb/CHANGELOG.md`.
