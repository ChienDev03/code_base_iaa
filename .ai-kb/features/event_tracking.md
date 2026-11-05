# Event tracking

This base defines no approved product event catalog. Previous product CSVs, screen names, aliases,
SDK hooks and historical logs are not approval to emit events in a new application.

## Approval and ownership

Before implementation, approve each event's name, trigger, parameters, types, attribution, privacy
requirements and deduplication boundary. Use feature-owned intent methods and a shared tracker
interface. Screens and ViewModels must not assemble arbitrary payloads. Optional analytics SDKs
must be absent from disabled dependencies, manifests and startup paths.

Navigation owns approved screen visibility reporting. Define exceptions for sheets and SDK UI
explicitly. Loading, impression, click, presentation and reward callbacks describe different facts;
map only the callback that proves the approved event. Capture source identity at event time.

## Payment and ads

Use the [premium and subscription](premium.md) provider outcomes for approved payment events.
Purchase, restore and reconciliation are distinct; do not retain backend-status sync events or
infer renewals from a first active snapshot. Revenue tracking and attribution SDK initialization
are separate opt-in integrations, not automatic payment requirements.

Deduplicate callbacks within the approved operation/context. Persist event claims only when
required; document crash/delivery limitations rather than promising exactly-once external delivery.

## Privacy and verification

Do not log personal data, receipts, tokens, credentials or device identifiers. Debug diagnostics
must not leak full provider/backend payloads. An event name alone is insufficient verification:
check timing, parameters, retries, owner destruction and duplicate delivery. No runtime analytics
validation or automated tests are claimed by this documentation.

## Small example catalog

The following rows illustrate documentation fields only. They do not authorize event emission.
Replace them with individually approved entries when a consuming product defines tracking.

| Example event | Trigger | Example fields | Deduplication |
| --- | --- | --- | --- |
| `example_screen_viewed` | Approved destination becomes visible | `screen_name` | Visibility owner |
| `example_purchase_completed` | Provider confirms purchase completion | `product_id`, `currency_unit` | Purchase operation |
| `example_restore_completed` | Provider confirms restore outcome | `result` | Restore operation |

A feature-owned tracker can expose typed methods rather than accepting arbitrary payload maps:

```kotlin
/** Example feature-owned analytics boundary; no transport is implied. */
interface ExampleTracker {
    /** Reports an approved feature visibility transition. */
    fun screenViewed()

    /** Reports an approved action result using a non-personal category. */
    fun actionCompleted(result: String)
}
```

Until a catalog is approved, leave hooks unconnected or use safe debug-only mock diagnostics.
Do not copy former feature aliases, source CSV links or transaction/identity data into this base.
