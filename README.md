# Local Android base

Single-module Kotlin/Compose Android base: `com.your.sourbase` and dev ID `com.your.sourbase.dev`.
Financial data stays on-device in Room/SQLite. DataStore owns small preferences.
Ads and optional Firebase services may use the internet independently of local data access.

## Build

Use the Gradle wrapper with the existing SDK 37 setup. The daemon criteria pin JDK 25.
On Windows: `./gradlew.bat assembleDevDebug assembleProductionDebug lintDevDebug`.
Android Studio: select `devDebug` or `productionDebug` and the shared app run configuration.
Release bundles remain unsigned unless the owner supplies signing configuration.
`SANDBOX` controls the eventual ads test policy; it is independent of the build type.
Optional `.env.dev` / `.env.production` configure SANDBOX and APP_NAME; CI values take precedence.

## Included

- Compose design system, Navigation 3, Hilt, immutable ViewModel state and buffered effects.
- Room database, categories, budgets, transactions, computed budget totals and local repository.
- Integer minor-unit money, Unix-second timestamps, foreign keys, schema export, WAL.
- DataStore settings, injected clock/dispatchers, safe local failure results.
- AdsManager boundary and lifecycle/configuration documentation. No ads SDK is installed yet.
- Firebase Analytics, Crashlytics and Remote Config adapters behind `-PenableFirebase=true`.
- Rating and update reference contracts; their UI flows are not implemented.
- Existing CircleCI/Fastlane/distribution scripts retained unchanged.

The example screen still renders neutral local demo items. Financial storage is available through
`LedgerRepository`; no financial screens or sample financial records are generated.

## Firebase

The default build has no Firebase runtime dependencies or providers.
To enable, supply your own ignored `app/src/dev/google-services.json` and
`app/src/production/google-services.json` for the respective application IDs, then build with
`-PenableFirebase=true`. Do not copy another app's configuration.

Analytics and crash collection default to disabled. The product can use `TelemetryControl` after
its collection policy is decided. No product events are approved or emitted in this starter.
RemoteSettings provides immediate cached/default values and an explicit best-effort refresh;
startup and database operations never wait for it. No financial values are passed to these services.
See [.ai-kb/firebase.md](.ai-kb/firebase.md).

## Local database and migration

See [.ai-kb/local-storage.md](.ai-kb/local-storage.md). Room schema JSON under `app/schemas` is
versioned source. Future schema changes need a version increment and explicit migration.
No destructive fallback is enabled. No existing database import is assumed.
Cloud backup/device transfer remain disabled by the existing manifest policy.

## Verification and maintenance

`./gradlew.bat :app:dependencies --configuration devDebugRuntimeClasspath`
inspects the selected runtime graph. Build/lint/compile are allowed. Unit, device, Maestro and
benchmark execution require explicit authorization; compiling test sources does not run them.
Start at [.ai-kb/README.md](.ai-kb/README.md).

Rename preview: `python3 scripts/change_bundle.py --new-package com.your.app --app-name "Your App" --dry-run`.
Review the preview before applying. Never overwrite signing identities or Firebase configs.
