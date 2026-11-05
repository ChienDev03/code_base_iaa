---
name: feature-scaffold
description: Add a new feature vertical (screen/destination) to this Android app the way the existing ones are built. Use when a task asks to create a new screen, feature, tab, flow or Navigation 3 destination under com.lutech.pairpulse.features, replace a destination explicitly confirmed to be a placeholder, or wire a new repository + ViewModel + route + Hilt binding + tests end to end. Covers the exact package layout, the AppViewModel/UiState/UiEvent contract, Hilt binding placement in core/di/RepositoryModule, Routes.kt + AppRouteSerializersModule + AppNavGraph wiring, the hand-rolled-fake test convention, and the required .ai-kb documentation update.
---

# Scaffold a feature vertical

Feature-first single module, Google UDF layers: **UI → Data**. A `domain/` layer is added **only**
when logic is reused or must leave an oversized ViewModel (for example
`features/connect/domain/`) (`AnswerSubmitter`), so do not create one speculatively. The **architecture** skill is the deep-dive reference; `features/home` is the closest
complete model to copy (`features/premium` is the model for a repository with real side effects).

## 1. Package layout

```
features/<name>/
├── data/
│   ├── model/<Name>Models.kt          // data classes / enums, no Compose or Android UI types
│   └── repository/
│       ├── <Name>Repository.kt         // interface — what the UI needs, nothing more
│       └── <Name>RepositoryImpl.kt     // @Singleton @Inject constructor; name it InMemory<Name>Repository while faking data
└── presentation/
    ├── <Name>Contract.kt               // UiState + UiEvent + Callbacks (+ UI-only enums/models)
    ├── <Name>ViewModel.kt              // @HiltViewModel, extends AppViewModel
    ├── <Name>Screen.kt                 // stateful <Name>Screen + stateless <Name>Content + @AppPreviews
    └── components/                      // feature-private composables (only if the screen needs them)
analytics/<Name>Tracker.kt              // (under features/<name>/) feature-owned tracker, added with the first event only
```

Presentation-only features are legitimate when they need no repository. Auth currently has a real
data layer; do not use it as a no-data example.

## 2. Write the code

Read `references/templates.md` in this skill for copy-ready templates of every file
(contract, ViewModel, screen, repository, unit test, instrumented test).

Non-negotiables:

- ViewModel extends `AppViewModel<XUiState, XUiEvent>(XUiState())` from `core/base`; mutate via
  `updateState { it.copy(...) }`, emit one-shots via `postEvent(...)`. Never expose a `MutableStateFlow`.
- `XUiState : UiState` is an immutable data class with defaults; `XUiEvent : UiEvent` is a sealed
  interface of *navigation/one-shot effects only* — user-facing messages go through
  `SnackbarController`, not events.
- Repository methods either return data or throw; the ViewModel wraps calls in `runCatching`.
  For network-backed repositories extend `core/base/AppRepository` and use `safeApiCall` /
  `cacheThenNetwork`, which return `ApiResult`.
- Collect `AppViewModel.events` through `flowWithLifecycle(..., Lifecycle.State.STARTED)` so
  navigation effects are deferred while a screen is stopped; never use a bare screen collector.
- Inject `TimeProvider` (`core/time`) instead of calling `Instant.now()` / `LocalDate.now()` —
  tests depend on it being fake-able.
- Log every complex business function with `AppLog.tag("<Name>ViewModel").d(...)` (debug-only):
  action entry, result, error branch. Small UI functions may skip it. Never log tokens, bodies, or
  personal data.
- Events go through `<Name>Tracker`: unspecified events log mocks; only individually approved
  catalog events may use the composite Tracker inside that tracker. Never call Adjust directly.
- Data screens get pull-to-refresh: `isRefreshing` in state, `onRefresh()` in the ViewModel,
  `AppPullToRefreshBox` in the screen. First load = skeleton; refresh keeps content.
- Errors: backend message first (`error.toUiText(R.string.<name>_<action>_error)`), then the
  feature string, then the generic one — see the architecture skill `references/networking.md`.
- All UI follows the **compose-design-system** skill (tokens, `core/ui` catalog, no Material 3
  `Button`/`TextField`/`AssistChip`). Read it before writing the screen.

## 3. Wire it up (four files, easy to forget)

1. `core/di/RepositoryModule.kt` — add `@Binds abstract fun bind<Name>Repository(impl: <Impl>): <Name>Repository`.
   Repository bindings go **only** here; `CoreModule`/`ApiModule`/`DataStoreModule`/`TimeModule` are
   for infrastructure `@Provides`.
2. `navigation/Routes.kt` — add `@Serializable data object <Name>Route : ScreenRoute` with a
   snake_case `screenName` (that string is what reaches analytics).
3. `navigation/Routes.kt` → `AppRouteSerializersModule` — add
   `subclass(<Name>Route::class, <Name>Route.serializer())`. **Omitting this crashes on state
   restore, not at compile time.**
4. `navigation/AppNavGraph.kt` — add `entry<<Name>Route> { <Name>Screen(...) }`. If it replaces a
   placeholder, delete the `PlaceholderScreen` entry and its `R.string.placeholder_*` string.
   Screens take `hiltViewModel()` — `viewModel()` does not work with Navigation 3 entries here.
   If the destination is a bottom-bar tab, also add it to `AppTab` in `navigation/NavigationBar.kt`;
   if it must hide the bottom bar (like Paywall/Login), extend that condition in `AppNavGraph`.

Navigation happens by mutating the back stack in `AppNavGraph`: `backStack.add(route)`,
`backStack.removeLastOrNull()`, or `backStack.clear()` + `add(route)` for a root replacement.
Screens never own the back stack — they expose `onX: () -> Unit` lambdas.

## 4. Strings and resources

Every user-visible string goes in the module's own file `app/src/main/res/values/string_<module>.xml`
(e.g. `string_connect.xml`), with every key prefixed `<module>_`; cross-module strings go in
`string_common.xml`. Never add to the legacy `strings.xml` or another module's file. Never add `app_name`
(supplied per flavor via `resValue`). Anything env-driven goes through `.env`/`.env.dev` +
their `.example` templates and a `buildConfigField`.

## 5. Tests (required)

Convention is **hand-rolled fakes — no Mockito/MockK**.

- `app/src/test/.../features/<name>/presentation/<Name>ViewModelTest.kt` — `StandardTestDispatcher`
  + `Dispatchers.setMain`/`resetMain`, private `Fake<Name>Repository`, fake `TimeProvider`,
  a real `SnackbarController()`. Cover: initial load success, failure path, each one-shot event.
- `app/src/androidTest/.../features/<name>/presentation/<Name>ScreenTest.kt` — `createComposeRule()`,
  render `<Name>Content` inside `AppTheme`, assert rendering + callback dispatch.
- Optional E2E: **offer** the user a `.maestro/<name>.yaml` flow for the new screen; create it only
  if they accept.

Tests are run manually — list these commands and ask the user before running any of them
(a compile check `./gradlew compileDevDebugKotlin` is fine without asking):

```bash
./gradlew testDevDebugUnitTest --tests "com.lutech.pairpulse.features.<name>.*"
./gradlew connectedDevDebugAndroidTest    # needs a device/emulator
./gradlew assembleDevDebug
```

## 6. Documentation (mandatory, per AGENTS.md)

1. Create `.ai-kb/features/<name>.md` — UDF state, data source, actions/events, and any temporary
   fake-data boundary. Follow the shape of `.ai-kb/features/home.md`.
2. Link it from `.ai-kb/README.md`.
3. Add a dated entry to `.ai-kb/CHANGELOG.md`, and tell the user the changelog was updated.
4. If the vertical changes an architecture pattern (new DI module, new base type, new nav shape),
   update the **architecture** skill (`SKILL.md` invariants + the matching `references/*.md`) and
   update `AGENTS.md` only if a mandatory repository rule changes. Run the KB validator.

## Completion checklist

- [ ] `data/model` + `data/repository` (interface + impl) or a documented presentation-only slice
- [ ] `<Name>Contract.kt`, `<Name>ViewModel.kt`, `<Name>Screen.kt` (stateful + stateless + `@AppPreviews`)
- [ ] `@Binds` in `RepositoryModule`
- [ ] Route object **and** `subclass(...)` in `AppRouteSerializersModule`
- [ ] `entry<...>` in `AppNavGraph`; placeholder + its string removed; `AppTab` updated if it is a tab
- [ ] Strings in `string_<module>.xml` with `<module>_` key prefix; no hardcoded UI text
- [ ] Each UI file < 500 lines; ViewModel < 1000 lines (split into section files / use cases)
- [ ] Unit test + instrumented test, no mocking library
- [ ] `.ai-kb/features/<name>.md`, `.ai-kb/README.md` table, `.ai-kb/CHANGELOG.md`
- [ ] Pull-to-refresh, `overscrollEffect = null` on scrollables, debounced raw clickables, file-private sizes
- [ ] `AppLog.d` on logic paths; feature-owned `<Name>Tracker` with approved events or mock fallbacks
- [ ] `./gradlew compileDevDebugKotlin` passes; test commands handed to the user (run only if they confirm)
- [ ] Design-token check run on the touched UI files
