# Premium and subscriptions

One reference for PaymentKit integration, paywall behavior, Restore Purchases and the subscription
screen. Names and product IDs below are examples. The integration layout is retained from the
reference implementation; check the selected PaymentKit artifact for exact supported APIs.
This document does not install the SDK or change application code.

## Integration map

```text
app/libs/PaymentKit-<version>.aar
core/di/PaymentModule.kt                SDK construction and configuration
core/payment/PaymentManager             one facade over SDK state and operations
core/payment/RestorePurchasesUseCase     shared restore action and feedback
core/payment/PaymentSessionSync          provider reconciliation and context invalidation
features/premium/
  data/model/PaywallConfig.kt            offer configuration
  data/repository/PremiumRepository      configuration and legal URLs
  presentation/PaywallContract.kt        immutable UI state and effects
  presentation/PaywallViewModel.kt       selection, purchase, restore and close actions
  presentation/PaywallScreen.kt          stateful entry and stateless content
  presentation/SubscriptionScreen.kt     current plan and management presentation
```

These paths describe responsibilities, not a requirement to create empty files. Subscription
presentation shares the payment facade with the paywall; it has no second entitlement repository.

### Dependency example

```kotlin
// app/build.gradle.kts — enabled payment integration only
implementation(files("libs/PaymentKit-<version>.aar"))
```

A local AAR has no POM. Declare the runtime dependencies required by that artifact explicitly;
do not copy old Billing, networking or Adjust versions without checking compatibility. Disabled
payment builds exclude the AAR, its transitive SDKs and manifest entries, not just startup calls.

## Updated payment boundary

The app no longer depends on backend subscription verify, status or restore APIs. Do not pass
`BuildConfig.BASE_URL` or the signed/authenticated application HTTP client for those operations.
Do not retain backend verification retry or backend status polling after login/foreground/restore.
PaymentKit must support the selected provider-based mode; the previous backend-dependent AAR
cannot be assumed compatible. Exact construction and reconciliation APIs depend on that artifact.

Retain the useful facade responsibilities from the reference integration:

| Facade responsibility | Reference API names / meaning |
| --- | --- |
| Shared entitlement | `subscription`, `isPremium`; distinguish unresolved from confirmed inactive |
| Current plan metadata | `subscriptionDetails`; period, localized price and supported renewal data |
| Operation ownership | `isProcessing`; prevents conflicting purchase and restore actions |
| Product catalog | `products`, `loadProducts`; configured intro/in-app products |
| Store operations | `purchase`, `restore`; delegate to SDK, without app-backend payment calls |
| Reconciliation | Provider-supported owned-purchase / entitlement refresh; no assumed replacement method name |
| Outcomes | `events`; normalize the actual SDK's terminal and pending outcomes |

SDK method names must be checked before implementation. In particular, `retryVerification` and
backend `/status` are not part of the new app contract. A failed reconciliation or absent plan
metadata must not be interpreted as confirmed free access. Apply only provider-supported caching.

## Remote Config: `paywall_config`

The reference parser maps a blank or malformed payload to a safe configuration result rather than
throwing. Configuration controls offers and display; it does not grant entitlement.

| Field | Meaning |
| --- | --- |
| `backgroundTriggerThresholdSeconds` | Optional background-return trigger interval; null disables that trigger |
| `packages.intro` / `packages.inapp` | Offer groups for intro and ordinary entry points |
| `items[].enabled` | Whether the configured offer is included |
| `items[].id` | Store product ID |
| `items[].name` | Optional display-name override; blank falls back to product metadata |
| `items[].location` | Optional group filter |
| `items[].level` | Ascending display order |
| `packageDefaultId` | Initial selection; fall back to the first valid loaded offer |
| `closeButtonDelaySeconds` | Configured Close delay; apply only if the product approves that behavior |

```json
{
  "backgroundTriggerThresholdSeconds": 600,
  "packages": {
    "inapp": {
      "closeButtonDelaySeconds": 0,
      "packageDefaultId": "example.inapp.yearly",
      "items": [
        {"enabled": true, "id": "example.inapp.monthly", "name": "Monthly", "location": "inapp", "level": 1},
        {"enabled": true, "id": "example.inapp.yearly", "name": "Yearly", "location": "inapp", "level": 0}
      ]
    },
    "intro": {
      "closeButtonDelaySeconds": 0,
      "packageDefaultId": "example.intro.yearly",
      "items": [
        {"enabled": true, "id": "example.intro.yearly", "name": "Yearly", "location": "intro", "level": 0}
      ]
    }
  }
}
```

`PaywallConfigParser` → `PremiumRepository.loadPaywallConfig()` →
`PaymentManager.loadProducts(...)` → offers for the current location → selected product.
Preserve a user selection while its product remains available. Disabled or unavailable products
must not leave a purchasable stale selection.

A `PaywallPreloader` can preload configuration and products before entry. Coalesce concurrent
loads, reuse successful results and let an explicit retry force refresh. Do not introduce eager
startup work merely to reproduce the reference layout.

## Paywall presentation and events

Reference composition: `PaywallScreen` collects state and buffered effects at lifecycle STARTED;
`PaywallContent`, plan cards and actions render immutable state. The ViewModel depends on
repository interfaces. A small serializable route carries the offer location and an example entry
point such as `settings` or `locked_content`, rather than old product feature names.

| State / outcome | Reference handling |
| --- | --- |
| Initial products loading | Plan and CTA skeletons; restore availability follows the operation policy |
| Loaded products | Localized prices, valid selection, purchase action and legal links |
| No products / failed load | Explicit empty/error state; confirm retry and footer behavior before implementation |
| Purchase or restore active | Claim once; disable duplicate and conflicting actions |
| Purchase cancelled | Keep the current screen; do not grant access |
| Purchase pending | Show the approved pending state; do not report completed access |
| Purchase completed | Apply provider-confirmed entitlement, then deliver one approved success effect |
| Already owned / ambiguous failure | Reconcile existing provider purchase before allowing another charge |
| Product / store failure | Approved failure copy and recovery action; never show exception text |

The old verification-failure sheet branch is removed. A retry resumes/reconciles the provider
operation where supported; it must not blindly repeat a possibly completed charge. Close, Back,
loading overlays, success sheets and automatic dismissal are UX choices to confirm, not inherited
requirements. Clear focus and dismiss the previous editor's IME on paywall entry if that behavior
is approved; preserve its draft.

Use SDK price/currency/period metadata, including trial eligibility. For comparison pricing, derive
a yearly-to-monthly equivalent only for supported positive prices and periods, format it with the
same currency rules and label it as an equivalent. Do not infer billing periods from SKU text.

Legal links can use a small remote configuration object:

```json
{"privacy": "https://example.com/privacy", "terms_of_services": "https://example.com/terms"}
```

Validate HTTPS destinations and use configured fallbacks. Link launch failures use the shell
snackbar. Do not retain old product URLs, artwork, partner names, benefit copy or Figma node IDs.

## Shared Restore Purchases

Paywall and settings call the same `RestorePurchasesUseCase`; the use case delegates to
`PaymentManager.restore()`, updates shared SDK-backed state and delivers feedback once. It does
not call an app-backend restore endpoint or perform a backend status follow-up.

| Normalized result | Handling |
| --- | --- |
| Restored eligible access | Publish confirmed access; one success message and approved navigation |
| Nothing eligible | Approved not-found message; keep the user on the current destination |
| Failed | Provider-safe copy or feature fallback; retain recoverable entitlement state |
| Pending / cancelled | Follow provider-supported outcome and approved UX; do not claim restored access |
| Busy | Ignore duplicate request; retain the active operation |

These are app concepts, not guaranteed SDK enum names. Preserve typed provider failure details
internally for recovery, without exposing receipts or tokens. Do not create a second snackbar in
the screen when the shared use case already owns feedback.

## Subscription screen

Reference structure: `SubscriptionScreen` → `SubscriptionContent` → `SubscriptionCard`. Observe
`PaymentManager.subscriptionDetails` and entitlement from the same facade used by the paywall.

| Display field | Source / fallback |
| --- | --- |
| Plan title | Provider product metadata or configured display name |
| Price and period | Localized provider price and actual billing period |
| Benefits | A few app-owned example strings, configured per supported plan |
| Renewal / expiry / auto-renew state | Provider-supported metadata; omit unavailable facts |
| Manage subscription | Provider/store management destination when available |

Render renewal copy from SDK renewal metadata if supported; the reference used
`SubscriptionDetails.renewal` and emphasized date segments. Do not force US dates, treat every
unknown plan as yearly, or invent expiration from purchase time. Lifetime access is not renewable.
Missing details or unresolved entitlement alone must not dismiss the screen. Confirm behavior for
confirmed expired/inactive access and returning from store management before implementation.

## Lifecycle, ads and tracking

Coalesce provider refreshes and guard results by current context/generation. Confirm how app login
relates to store identity; app logout does not necessarily remove a store purchase. Recomposition
must not duplicate products, purchase callbacks or reconciliation.

Ads use the shared confirmed entitlement: if only free users may see ads, unresolved access keeps
ads disabled. There is no backend-status gate. Any automatic paywall policy must likewise wait
for confirmed inactive access and avoid active purchase/restore/fullscreen-ad flows.

Keep payment event hooks behind feature-owned tracking methods. Purchase, restore and refresh are
separate facts; do not infer renewal from a first active snapshot. Adjust/revenue reporting is an
explicit optional integration, not automatically enabled by PaymentKit. See [tracking](event_tracking.md).

## Verification

Review duplicate taps, purchase pending/cancelled, already-owned recovery, restore with/without
eligible purchases, offline cached access, context replacement, stale callbacks, subscription
metadata and no remaining backend payment calls. Unit, instrumented and Maestro runs require
explicit confirmation. Reference scenarios and API names are not claims of executed SDK tests.
