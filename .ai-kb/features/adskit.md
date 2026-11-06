# Ads integration

Reusable guidance for an optional AdsKit adapter. SDK installation, version and supported APIs
must be checked in source before implementation. No product placements or shipped behavior are
inherited from a previous application.

| Task | Contract |
| --- | --- |
| SDK boundary, startup and resource ownership | [SDK integration](adskit/sdk.md) |
| Placement configuration and test mode | [Configuration](adskit/configuration.md) |
| Rewarded and fullscreen lifecycle | [Lifecycle](adskit/rewarded.md) |

The app owns placement policy and feature continuation. The SDK owns consent and ad lifecycle
according to its supported APIs. Ads eligibility uses app policy, consent, configuration and host readiness. Local data access never waits for ads.
Disabled ads must be absent from dependencies, merged manifests and startup/DI paths.
