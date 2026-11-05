# Base Example
Neutral single-module Android starter: `com.example.base`, dev ID `com.example.base.dev`.
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
1. Choose a new empty destination; copy only reviewed tracked starter source, never keys/caches/.git.
2. Decide app name and namespace/application ID; edit app Gradle namespace/ID/resValue and move the
   `com/example/base` source/test folders with IDE package refactoring. Review manifest component names.
3. Replace `BrandPalette` in `ui/theme/Theme.kt` and app-owned launcher. Check light/dark, contrast,
   disabled/pressed/error states. Keep generic primitives separate and fontScale=1f.
4. Copy environment examples locally; select offline/public mode and independent sandbox values.
5. Select integrations from `.ai-kb/integrations.md`. SDK presets are currently blocked pending
   authorized playbooks/registry; no undocumented manual SDK enabling is claimed supported.
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
Start at `.ai-kb/README.md`; `BASE_SOURCE_PLAN.md` records outstanding acceptance gates.

Validated source state: debug APK and unsigned minified release AAB build, lint has no errors,
6 Kotlin + 4 Python unit tests pass, UI tests compile but have not run. See BASE_SOURCE_PLAN.md
for every checklist item's status, artifacts and remaining SDK/device/live-adapter prerequisites.
