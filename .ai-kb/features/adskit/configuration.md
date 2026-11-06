# AdsKit configuration and placement control

Back to [AdsKit](../adskit.md). Keep the integration schema and mapping explicit; use only a few
example placements instead of the previous product's feature list. SDK names below are reference
APIs and must be checked against the selected artifact.

## Local units: `res/raw/ads_units.json`

```json
{
  "appId": "<configured-app-id>",
  "testDeviceIds": [],
  "placements": [
    {"remoteConfigId": "example_banner", "adUnitId": "<configured-banner-unit-id>"},
    {"remoteConfigId": "example_rewarded", "adUnitId": "<configured-rewarded-unit-id>"}
  ]
}
```

Placeholders intentionally fail real SDK validation until configured. Do not copy production
ad IDs or team device IDs from another app. Test-device values belong in approved local/CI inputs.

Reference model mapping:

| Remote ID | Example `AdPlacement` | Format |
| --- | --- | --- |
| `example_banner` | `ExampleBanner` | Banner |
| `example_rewarded` | `ExampleRewarded` | Rewarded |

`AdPlacement` owns stable app placement identity; `PlacementSpec` maps the remote/local config to
SDK placement ID and format. Banner size is a UI choice, not another product feature ID.

## Remote Config: `ads_config`

The RemoteSettings adapter now provides this key through Firebase or SDK-free defaults.
Bundled defaults contain empty placement arrays. The parser and AdsKit adapter are not installed.
A refresh must never block local financial data access.

Reference flat payload:

```json
{
  "banner": [{"enabled": true, "placementId": "example_banner"}],
  "rewarded": [{"enabled": false, "placementId": "example_rewarded"}],
  "interstitial": [],
  "native": []
}
```

The reference loader also supports an `{"ads": {...}}` envelope by unwrapping before decoding.
Keep support only if required by the actual configuration contract; do not silently decode an
unrecognized envelope into empty placement arrays.

Reference sequence:

```text
read local units → validate App ID → fetchAndActivate remote config
→ decode ads_config → map PlacementSpec → register AdsKit config
→ publish resolved placements → apply consent/host readiness
```

Fetch failure can read activated cache/defaults. Distinguish invalid JSON from explicitly disabled
or missing placements. The imported reference fell back to configured local placements on parse
failure; document and approve that fallback before using it in another app.

`enabledPlacements: StateFlow<Set<AdPlacement>>` plus
`isPlacementConfigLoaded: StateFlow<Boolean>` distinguish unresolved from resolved-empty config.
A pending action waits only under its current owner and ignores duplicates; cancellation releases
busy state. Confirm the local alternate action for disabled rewarded placements; never grant an unearned reward.

## Sandbox and test devices

The reference passes `BuildConfig.SANDBOX` into `AdsConfig.isTestMode`. AdsKit then substitutes
format-matching sample units internally before registration. Confirm the selected artifact actually
implements this behavior; a boolean alone is not evidence that live ads cannot be requested.

Explicit test-device forwarding is separate from sample-unit substitution and must work when
configured in either mode. Never log device IDs. The manifest App ID and runtime configuration
must agree where required by the SDK/consent integration. Disabled builds omit SDK dependencies
and manifest entries entirely.

## Adding a placement

1. Add its local unit entry and stable `AdPlacement` identity.
2. Add its `PlacementSpec` format/config mapping.
3. Add the remote config entry if remote control is used.
4. Handle unresolved, disabled and unavailable placement states in the feature owner.
5. Wire app policy, consent and usable host checks.
6. Verify disabled/enabled dependency and manifest variants with authorized checks.

This is documentation of configuration mechanics, not authorization to publish remote values.
