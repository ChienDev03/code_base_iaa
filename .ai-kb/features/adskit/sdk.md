# Ads SDK integration

Back to [ads integration](../adskit.md).

## Adapter and dependencies

Expose an app-facing ads interface and keep vendor lifecycle calls in its adapter. Feature
ViewModels depend on interfaces. Verify the actual SDK artifact, supported API, minimum platform
and transitive dependencies; this document does not pin a vendor version or claim APIs exist.
A local AAR may require explicit runtime dependencies because it has no published dependency metadata.
Do not add overlapping SDK implementations.

Optional disabled builds must exclude the SDK, providers, permissions and manifest metadata.
A runtime enable switch alone does not meet that requirement.

## Startup and readiness

Initialize once per supported configuration and coalesce concurrent work. Keep configuration
resolution, premium eligibility, consent, SDK initialization and placement readiness distinct.
Do not wait on backend subscription status: use the shared [premium repository](../premium.md).
When ads require inactive premium access, unresolved entitlement keeps requests disabled.

Follow the selected SDK's consent sequence. Provide privacy options when required by its consent
contract. Consent failure and cached-consent handling must follow that contract, without inventing
permission to request ads. Cancel obsolete startup work and apply the newest eligibility state.

## Resource ownership

- Bound banner caches and preload concurrency. Choose retention per placement, document it and
  invalidate resources on host destruction or policy changes as required by the SDK.
- Do not attach one native view to multiple hosts. Protect transfers with current-owner identity;
  an old host must not release or erase a newer host's binding.
- Keep placement and banner size separate. Cache keys must distinguish incompatible formats/sizes.
- Define placeholder, failure/no-fill and retry behavior before adding UI. Host re-entry should
  follow the approved cache policy rather than accidentally restart loading.
- Serialize fullscreen ownership and release it on every terminal path. Bound pending waits;
  a load timeout must not dismiss an already visible ad.
- Propagate cancellation, reject stale callbacks and avoid retaining destroyed activities.

## Events and verification

Use SDK-confirmed callbacks for shown, clicked, rewarded and paid-impression outcomes. Capture
source identity when the event occurs; do not infer it from current navigation after delayed delivery.
No event is authorized by this document. See [tracking](../event_tracking.md).

Review dependency graphs, merged manifests, startup order and cache ownership. Real consent,
creative rendering and ad outcomes require device verification; static checks cannot prove fill.
Tests and device automation require the repository's explicit authorization.

## Reference wiring and API map

```text
app/libs/AdsKit-<version>.aar
core/ads/AdsManager                  app-facing interface
core/ads/AdsKitAdsManager            delegate over the vendor SDK
core/ads/AdPlacement                 stable placement identity
core/ads/AdsConfigFactory            local/remote configuration mapping
core/ads/AdsPremiumSyncer            shared provider-backed entitlement → ads eligibility
core/di/AdsModule                    adapter and SDK construction
core/ui/components/ads/AppBannerAd   themed banner host
```

This layout and the APIs below are retained integration references, not assertions that these
files or methods exist in the current checkout. Verify them against the chosen artifact.

```kotlin
// Enabled integration only; use the artifact actually selected by the app.
implementation(files("libs/AdsKit-<version>.aar"))
```

| Reference API | Responsibility |
| --- | --- |
| `AdsKit.initialize(context, config, callback)` | Register configuration and initialize once when gates permit |
| `AdsKit.initializationState` | Observe deferred/loading/ready/failed initialization |
| `AdsKit.setAdsEnabled(enabled)` | Apply the latest premium/app policy, including before readiness |
| `AdsKit.gatherConsent(activity)` | Refresh consent and show a form if required |
| `AdsKit.consentState` | Observe consent and privacy-options requirements |
| `AdsKit.showPrivacyOptionsForm(activity)` | Show consent privacy options |
| `AdsKit.preloadBanner(placementId)` | Optional explicit banner demand |
| `AdsKit.disposeBanner(placementId)` | Release placement banner resources |
| `AdsKit.showInterstitial(activity, placementId, onDismissed)` | Present or terminate with one continuation |
| `AdsKit.showRewarded(...)` | Present and report supported reward/terminal callbacks |

The reference uses `AdsKitTheme` for the banner frame, badge and placeholder. Map Material roles
such as `surface`, `onSurface` and `onSurfaceVariant` into that theme; advertiser creatives and
fullscreen controls remain SDK-owned.

### Banner state and cache reference

`Idle → Loading → Ready | Failed`: idle/failed collapse, loading shows the approved placeholder,
and ready attaches the cached view. Returning to a host may reuse the cache without replaying
loading, subject to Activity validity, consent and retention policy.

`KEEP_LOADED` retains a reusable banner after host detach; `DISPOSE_ON_HIDE` releases it after
its final owner leaves. Cache by placement and supported size. Reference sizes include adaptive,
320×50, 320×100 and 300×250 dp; use only sizes actually supported by the selected SDK.
A `(placement, size)` cache key and per-host owner token prevent host transfers from stealing or
releasing another visible banner. Click attribution snapshots an explicit `sourceId` at callback
time rather than reading current navigation after dispatch.

Keep one current SDK version and its required runtime dependencies. Do not preserve the imported
SDK version pin, old migration dates or obsolete parallel configuration mechanisms.
