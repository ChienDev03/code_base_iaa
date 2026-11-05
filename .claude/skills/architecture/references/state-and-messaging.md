# Shared base types, one-shot events, and user messages

## `core/base`

`AppViewModel<S : UiState, E : UiEvent>` and `AppRepository` are the intended base
classes for **new** feature ViewModels/repositories.

- `AppViewModel` — `state: StateFlow<S>`, `events: Flow<E>` backed by `Channel.BUFFERED`, protected
  `updateState(transform)` and non-suspending `postEvent(event)` (`trySend`). Each one-shot event is
  received by exactly one collector. Never expose a `MutableStateFlow`.
- `AppRepository` — protected `safeApiCall(block)` (wraps a suspend call into `ApiResult`, mapping
  failures via `ApiErrorMapper`) and `cacheThenNetwork(cache, networkCall)` (emits the cached value
  first if present, then the network result).
- `UiState` / `UiEvent` are marker interfaces. `XUiState` is an immutable data class with defaults;
  `XUiEvent` is a sealed interface of **navigation/one-shot effects only**.

`PaywallViewModel` follows the same `AppViewModel` contract. Repository domain broadcasts may keep
their own `SharedFlow` when multicast semantics are intentional.

Avatar editing reuses `core/ui/components/photo/BaseAvatarCropViewModel`, an `AppViewModel`
subclass inherited by onboarding and About You. It owns source moderation through
`MediaUploadRepository`, immutable editor state, replacement/cancellation and buffered effects.
Feature subclasses override only save behavior (local crop or crop/upload/commit) and approved
successful-save tracking, keeping feature repositories independent.

## Why `Channel`, not `SharedFlow`

One-shot navigation effects and user messages use buffered `Channel`s exposed via `receiveAsFlow()`.
`postEvent` and `SnackbarController.show` stay non-suspending via `trySend`.

A channel **retains** a queued value while there is temporarily no collector and delivers it to
exactly one collector. `MutableSharedFlow(replay = 0)` drops emissions when no subscriber is
attached — `extraBufferCapacity` only helps existing slow subscribers. This matters during route
and configuration transitions, which is exactly when a navigation effect or snackbar is emitted.

Repository state/update broadcasts may still use `StateFlow` or `SharedFlow` when multicast
semantics are intentional.

Account deletion completion is durable app-shell state rather than a screen navigation event:
`SessionViewModel.accountDeletionCleanupState` progresses from `Idle` to `Cleaning` to
`ReadyForNavigation`. The Delete destination only sends a non-suspending success signal;
`AppNavGraph` observes the state and replaces the back stack after mandatory local cleanup. This
keeps cleanup independent of the destination collector's coroutine and survives configuration
recreation through the Activity-retained ViewModel.

Screen collectors for `AppViewModel.events` must be lifecycle-aware at `Lifecycle.State.STARTED`:

```kotlin
val lifecycle = LocalLifecycleOwner.current.lifecycle
LaunchedEffect(viewModel, lifecycle) {
    viewModel.events
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect(::handleEvent)
}
```

The buffered `Channel` retains events while the screen is stopped; collection resumes without
executing navigation callbacks in the background.

## App-scoped user messages (`core/ui/snackbar`)

The package separates **message production** from **rendering**:

1. A feature ViewModel injects the singleton `SnackbarController` (constructed straight from its
   `@Singleton @Inject` constructor — no `CoreModule` binding) and enqueues a `UserMessage` holding
   **unresolved** `UiText`:

   ```kotlin
   snackbar.show(text = UiText.Res(R.string.message_x), type = AppSnackbarType.Info)
   // Errors: backend message first, then feature string, then generic (see networking.md).
   // snackbar.show(text = error.toUiText(R.string.<feature>_<action>_error), type = AppSnackbarType.Error)
   // Never expose exception, transport, or billing diagnostic text to users.
   ```

   ViewModels never call the Compose-only suspending `showSnackbar`.
2. `AppSnackbarViewModel`, resolved with app-level `hiltViewModel()`, exposes the controller flow to
   `AppNavGraph`.
3. `AppSnackbarMessagesHost` resolves resources in composition and renders them through the
   **sole** `SnackbarHostState`, in the app `Scaffold.snackbarHost` slot. Its collector uses
   `showSnackbarsInOrder`: duplicate `UiText`, severity and action-label combinations are ignored
   while their rendering job is active (showing or explicitly waiting). Suppression lasts until
   completion, cancellation or dismissal, without a fixed timeout or extending the visible duration.
   Different default messages cancel the suspended `showSnackbar` call and all queued messages,
   immediately replacing the current snackbar (**latest wins**). Only `afterCurrent = true` waits
   for the prior snackbar; do not make default messages wait sequentially.

**No screen or destination may create or own a `SnackbarHostState`.** Because the host is a sibling
of `NavDisplay`, a message emitted on Login or Paywall stays visible after navigation.

Visual anatomy is documented in `.ai-kb/design-system.md` and the **compose-design-system** skill.
