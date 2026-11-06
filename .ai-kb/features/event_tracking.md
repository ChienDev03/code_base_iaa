# Event tracking

There are no approved product events in this starter. Firebase SDK installation does not approve
event names, payloads or collection. Collection defaults to disabled; see [Firebase](../firebase.md).

Feature-owned trackers may call AnalyticsProvider only after their event names, triggers,
parameter types, deduplication and privacy requirements are approved. Screens/ViewModels do not
build arbitrary payload maps. Until then, leave hooks unconnected or use safe debug-only diagnostics.

Never send financial values, transaction notes, lender names, SQL, identities or device IDs.
Ads impression/show/click/reward are separate facts and need SDK-confirmed callbacks.
Deduplicate within the operation owner; do not claim exactly-once external delivery.
Navigation reporting also needs an approved catalog; no automatic screen events are wired here.
