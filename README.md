# Base Example
Neutral single-module Android starter: `com.your.sourbase`, dev ID `com.your.sourbase.dev`.
Offline demo requires no private SDK, Firebase config, environment secret or release signing.

## Quick start
Use Android Studio with JDK 17+ as the launcher (the pinned daemon uses JDK 25) and SDK 37 (existing wrapper uses Gradle 9.4.1 / AGP 9.2.1).
Set ANDROID_HOME to your SDK or create ignored local.properties yourself.
Open the **base-source root folder**, sync Gradle, then select `app` in **Build Variants**:
`devDebug` for development or `productionDebug` for production configuration with debugging enabled.
Select the shared `app` run configuration and a device, then Run/Debug. If variants are missing,
resolve the first Gradle Sync error; both flavors are declared in `app/build.gradle.kts`.
In Settings > Build Tools > Gradle, use the Android Studio bundled JDK / `GRADLE_LOCAL_JAVA_HOME`.
The wrapper daemon criteria select JDK 25; allow its download or install that JDK locally.

Run `./gradlew assembleDevDebug`; install with `./gradlew installDevDebug`.
On macOS terminals without a configured Java runtime, use `./scripts/gradle.sh assembleDevDebug`;
the helper prefers an explicit JAVA_HOME and otherwise uses Android Studio's bundled runtime.
Build both debug variants with `./scripts/gradle.sh assembleDevDebug assembleProductionDebug`.
Development uses `.dev`, version suffix `-dev` and label `Base Example [DEV]`; production keeps
the base identity. Both debug variants use Android's generated debug signing, never source-app keys.
`APP_NAME` optionally overrides the label. `-PreleaseVersionCode=123` overrides the version code.
Default mode is offline. Optional local filenames: `.env.dev`, `.env.production`; sanitized examples
are supplied. CI process environment wins. `DEMO_MODE=public` opts into JSONPlaceholder read-only API.
`SANDBOX=true/false` is independent from debug/release. No API keys belong in BuildConfig.

## Rebrand/bootstrap (manual, reviewable)
Fast path: `python3 scripts/change_bundle.py --new-package com.your.app --app-name "Your App" --dry-run`
auto-detects the current namespace, previews every move/edit, then run without `--dry-run` to apply.
See **Rename the bundle** below for scope and limits.

1. Choose a new empty destination; copy only reviewed tracked starter source, never keys/caches/.git.
2. Decide app name and namespace/application ID; edit app Gradle namespace/ID/resValue and move the
   `com/your/sourbase` source/test folders with IDE package refactoring (or run `change_bundle.py`).
   Review manifest component names.
3. Replace `BrandPalette` in `ui/theme/Theme.kt` and app-owned launcher. Check light/dark, contrast,
   disabled/pressed/error states. Keep generic primitives separate and fontScale=1f.
4. Copy environment examples locally; select offline/public mode and independent sandbox values.
5. Any optional integration (Firebase, PaymentKit, AdsKit, Clarity, Adjust…) stays absent by default.
   SDK presets are blocked pending authorized playbooks/registry; no undocumented manual SDK enabling
   is claimed supported.
6. Dry-run the proposed edits with IDE preview/diff before applying. No global arbitrary replacement.
   Review packages/labels/provider/deep-link IDs, run static/compile/build/lint and inspect manifests/
   dependencies. Run tests only after explicit authorization. Do not overwrite signing identities.

## Opt-in matrix
| Capability | Default | Enabled path |
| --- | --- | --- |
| Offline example | Enabled | No configuration |
| Public sample API | Disabled | DEMO_MODE=public; network failure remains visible |
| DataStore settings | Available | Local only; no sensitive entitlement cache |
| Room / images / media / work | Absent | Add only with a justified feature + lifecycle contract |
| PaymentKit / AdsKit | Absent | Blocked pending registry/playbooks and app contracts |
| Firebase services | Absent | Separate integration selection and provider/plugin review required |
| Release handoff | Dry-run only | Live adapter/schema not yet configured |

## Validation/release
`./gradlew compileDevDebugKotlin assembleDevDebug lintDevDebug`
`./gradlew :app:dependencies --configuration devDebugRuntimeClasspath`
`./gradlew bundleProductionRelease` produces an **unsigned**, minified AAB. Supply your own signing
only for actual distribution; see keystore/README.md. Preserve mapping files for crash diagnosis.
Unit/device/Maestro/benchmark execution needs explicit approval. No commit/publication is implied.
Start at `.ai-kb/README.md`.

Validated source state: debug APK and unsigned minified release AAB build, lint has no errors,
Kotlin unit tests pass, UI tests compile but have not run.

## Rename the bundle (`scripts/change_bundle.py`)
In-place rename of namespace, applicationId, package folders and every textual reference.

Usage:
```bash
python3 scripts/change_bundle.py --new-package <com.your.app> [--app-name "Your App"] [--dry-run]
```

| Flag | Effect |
| --- | --- |
| `--new-package` (required) | Target package, dot-separated lowercase segments. |
| `--app-name` | Overwrites the `APP_NAME` fallback in `app/build.gradle.kts` and syncs `rootProject.name` in `settings.gradle.kts` with a slug of this name. |
| `--dry-run` | Print the plan without touching files. Always start here. |

Behavior:
- Auto-detects the current package from `app/build.gradle.kts` (`namespace = "..."`).
- Moves `app/src/{main,test,androidTest}/java/<old>/` → `<new>/`; prunes empty parent folders.
- Rewrites both dotted (`com.old.pkg`) and path (`com/old/pkg`) references in every
  `.kt .kts .xml .pro .md .sh`, skipping `build/`, `.gradle/`, `.idea/`, `node_modules/`, `.git/`.
- `.claude/scripts/check-design-tokens.sh` auto-detects the namespace at runtime, so it follows
  the rename without manual edits.

Does **not** touch: signing/keystore, `google-services.json`, `.env*`, manifest component names
beyond the package replacement, Gradle sync. After applying, run `./gradlew clean && ./gradlew :app:assembleDevDebug` to verify.
