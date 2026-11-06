# State and messaging

AppViewModel<S, E> exposes immutable StateFlow and buffered Channel-backed events.
updateState mutates state; suspend postEvent sends a one-shot effect. The current base has no
UiState/UiEvent marker interfaces. Collect events with lifecycle STARTED awareness.

AppRepository.local wraps suspend operations in DataResult and propagates cancellation.
Use localized feature/generic error resources, never exception or database text.

SnackbarController is the singleton buffered queue of nullable strings. Null requests the generic
resource fallback. AppNavGraph owns the sole SnackbarHostState and lifecycle-aware collector.
Screens must not create additional snackbar hosts. Preserve existing refresh and in-flight behavior.
