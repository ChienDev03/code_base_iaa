# Local core architecture

The application stores financial records locally. Data screens must remain usable without internet.
Ads failures and Remote Config refresh failures never block opening or editing local data.
No connectivity fullscreen gate, server-clock validation, API transport or authentication is present.

## Results and errors

Repositories extend AppRepository and call `local { ... }`, returning `DataResult<T>`.
Success carries data; Failure carries no exception, SQL, message or stored values.
Presentation chooses a localized feature fallback, then the generic resource. CancellationException
is always rethrown. Avoid catching JVM Errors. No automatic crash reporting for validation failures.

## Concurrency and ownership

Room suspend queries manage IO. Inject AppDispatchers for other blocking IO/CPU work.
Use Room transactions for multi-table reads or validation followed by writes.
Keep in-flight action guards, reject stale callbacks, preserve existing content during refresh.
AppViewModel owns immutable StateFlow and buffered Channel effects; collect with lifecycle awareness.
One shell-owned SnackbarController carries messages across destinations.

## Time

Inject TimeProvider. Database timestamps are Unix seconds; its epochMillis value is divided by 1000
at persistence boundaries. Device time is sufficient; there is no server time dependency.
See [storage](local-storage.md) and [Firebase](firebase.md).
