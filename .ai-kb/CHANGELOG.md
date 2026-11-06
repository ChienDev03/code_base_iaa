# Knowledge base changelog

## 2026-11-06 — Specialize the base for local financial data

- Removed application HTTP dependencies, public API demo/configuration, backend error parsing,
  payment contracts/artifact references and upload/premium guidance across source, skills and docs.
- Added safe local DataResult and kept cancellation, ViewModel, navigation and UI behavior.
- Added Room/SQLite entities, DAO, computed budget totals, local repository and schema export.
  Retained DataStore preferences (`darkTheme`, `currency`, `onboardingCompleted`) and disabled
  destructive migration/backup behavior.
- Added build-selected Firebase Analytics, Crashlytics and Remote Config adapters with SDK-free
  defaults, explicit telemetry controls and nonblocking cached/default configuration; updated
  `scripts/change_bundle.py` to include `src/local/java` and `src/firebase/java`.
- Removed purchase/paywall prerequisites and server reward fulfillment from ads, update and
  rating/review contracts.
- Retained AdsManager, Rating/Update references and existing CI/CD files.
- Updated architecture, design-system and scaffolding references to match the actual local source.
- Both local debug builds, minified release bundle, lint and test-source compilation passed.
  See [verification](local-base-verification.md) for Firebase/configuration and runtime limits.

## 2026-11-05 — Fix example startup scroll constraints

- Removed the redundant verticalScroll modifier from the example LazyColumn. It supplied
  infinite height constraints to the lazy list and caused the reported startup crash.
- Preserved lazy scrolling, disabled overscroll, keyboard padding and pull-to-refresh behavior.
- Added the bounded lazy-list constraint rule to the design-system contract.

## 2026-11-05 — Cover two IDE unit-test tooling sample aliases

- Added exact Android-component SHA-256 entries for ui-tooling and ui-tooling-preview 1.11.2
  sample sources from the reported unit-test runtime classpath.
- Both cached files matched official parent-module Google Maven SHA-1 sidecars.
- Strict verification remains enabled; IDE UI sync and test execution remain unverified.

## 2026-11-05 — Cover six IDE runtime sample aliases

- Added the six exact Android-component sample checksums from the runtime-classpath report:
  Compose UI 1.11.2 (ui, graphics, test, text, unit) and window-core 1.5.0.
- Verified each cached file against its published parent-module Google Maven SHA-1 sidecar,
  then checked the resulting component-specific SHA-256 pins. Strict verification remains enabled.
- Broader cache-based metadata updates were not performed. IDE UI sync remains unverified.

## 2026-11-05 — Pin IDE Android sample aliases

- Added 24 SHA-256 entries under the exact Android component coordinates reported by IDE sync.
  Each cached file matched its published parent-module Google Maven SHA-1 sidecar.
- Kept strict verification enabled and the existing wrapper version.
- Linked `.agents -> .claude` as requested; preserved the previous directory in a temporary backup.

## 2026-11-05 — Cover every published documentation variant file

- The next IDE sync failed on 26 AndroidX `-samples-sources.jar` files, a second file inside the
  `sources` variant that earlier passes missed.
- Scanned the `.module` documentation variants of every pinned component and added all 311 files
  without checksums (samples, multiplatform, plugin/UTP sources and Javadoc). Each matched its
  official SHA-1 sidecar first; existing entries unchanged; no wildcard trust.
- Re-scan reports zero uncovered documentation files; strict root+app resolution and both debug
  builds passed. IDE UI sync not verified.

## 2026-11-05 — Cover IDE plugin-classpath Javadoc resolution

- Android Studio sync kept failing dependency verification, so the Build Variants panel was empty.
  The prior pass covered application classpaths only; root buildscript Javadoc was still missing.
- Resolved binaries, sources and Javadoc for root and `:app` in one build. Added 85 plugin-classpath
  Javadoc SHA-256 entries; each cached file first matched its official Google Maven / Maven
  Central SHA-1 sidecar. Existing entries unchanged; no wildcard trust or verification bypass.
- Repeat check: 808 documentation artifacts (532 components) passed strict verification;
  assembleDevDebug and assembleProductionDebug passed. IDE UI sync and device launch not verified.

## 2026-11-05 — Cover IDE application source and Javadoc resolution

- Latest IDE sync failed on 11 missing Javadoc checksums in the Android test compile classpath.
- Resolved source/Javadoc artifacts across all resolvable application compile/runtime classpaths,
  including test classpaths without executing tests. Added 226 artifact checksum entries and
  independently matched every added file against official repository SHA-1 sidecars. Existing
  checksum values were preserved; no wildcard trust or verification bypass was introduced.
- Repeated resolution with strict verification: 229 documentation artifacts across 202 components
  resolved successfully. Both debug builds passed. IDE UI sync and device launch remain unverified.


## 2026-11-05 — Verify Kotlin Gradle variant source artifacts

- The next IDE sync exposed five additional Kotlin plugin `gradle813-sources.jar` artifacts.
  Compared each with its Maven Central SHA-1 sidecar and added its SHA-256 checksum.
- Resolved all five through Gradle with verification enabled; both debug APK builds passed.
  Direct IDE sync verification was unavailable because Computer Use permission was not granted.


## 2026-11-05 — Verify Android Studio plugin source downloads

- Added SHA-256 checksums for 111 source JARs requested during IDE sync, after matching each
  cached file against its official Google Maven or Maven Central SHA-1 sidecar. No verification
  bypass or wildcard trust was added; existing checksums are unchanged.
- Verified all 111 source artifacts through a temporary Gradle resolution task with verification
  enabled; assembleDevDebug and assembleProductionDebug passed. No tests or IDE/device run.
- Documented the independent macOS protected-folder warning and scoped access guidance.


## 2026-11-05 — Fullscreen core blocking states

- Replaced offline/incorrect-time dialog references with opaque fullscreen, centered status screens.
- Added shell-owned system/predictive Back interception and app navigation/pop gating, while
  preserving the previous navigation state and allowing explicit settings access.
- Defined offline-before-time priority, recovery behavior and manual verification cases.
- Documentation only; application source and runtime behavior are unchanged.

## 2026-11-05 — Complete starter debug setup

- Preserved dev/production flavors and default debug signing; restored dev version/label distinction
  and configurable release version code / app label without importing source-product identifiers.
- Added a shared Android run configuration, local IDE Gradle JDK selection and a macOS terminal
  launcher fallback using the installed Android Studio runtime.
- Documented variant selection, JDK 25 daemon requirements and both debug build commands.
- Verification: assembleDevDebug, assembleProductionDebug, lintDevDebug and lintProductionDebug
  passed using scripts/gradle.sh; shell syntax and run/IDE XML checks passed. No tests or device run.
- Regression catalog SDK/action/lifecycle cases: N/A (build configuration only). Missing local
  regression/performance skill targets were observed; no runtime behavior changed.


## 2026-11-05 — Import supplied core, update and review specifications

- Added the full user-supplied connectivity/time/error-handling specification as
  `core_modules_specification.md` and Play rating/review as
  `features/app_rating_review_specification.md`.
- Replaced the reconstructed update reference with the full supplied
  `features/app_update_specification.md`, including its snake_case JSON schema, code and lessons.
- Preserved source-language prose and code blocks; normalized package/feature identifiers to
  `com.example.app` and `Example`, and corrected inconsistent update checklist class names.
- Added English adaptation notes for cancellation, tokens, error precedence, recovery ownership
  and review attempt semantics without modifying application code or installing dependencies.
- Linked all three contracts from the knowledge index and to each other. Verification is limited
  to document integrity, JSON, local links and example naming; no runtime/test/live operations.

## 2026-11-05 — Complete app update reference

- Expanded the app update contract with configuration fields and JSON, fallback sequencing,
  exact force/recommend precedence, numeric comparison examples, per-ViewModel checks, persisted
  ignore behavior, dialog actions, store return behavior and a verification matrix.
- Distinguished retained reference behavior from proposed schema fields and unavailable parser
  details; no installed update implementation or runtime verification is claimed.

## 2026-11-05 — Restore practical integration references

- Corrected the overly broad documentation cleanup: restored concrete integration maps, code
  snippets, configuration field tables, JSON payloads and lifecycle examples with neutral names.
- Kept only a few example feature/product/placement fields instead of former product catalogs.
- Premium and subscription remain one contract, with provider-backed purchase/restore and no
  application-backend verify/status/restore dependency. SDK reference names are labeled explicitly.
- Application source and runtime configuration are unchanged. This is a reconstructed reference
  from available prior documentation, not a byte-for-byte restoration of the imported history.

## 2026-11-05 — Separate design token files

- Extracted AppSpacing, AppShapes and AppMotion from Theme.kt into matching files in the same
  ui/theme package. Token values, imports at call sites and runtime behavior are preserved.
- Updated the design system contract to document token ownership.

## 2026-11-05 — Reusable base contracts

- Replaced imported product-specific documentation with reusable base integration guidance.
  Removed old branding, package/source paths, production ad/device identifiers, feature-specific
  policies and links to absent contracts.
- Consolidated premium, paywall and subscription presentation into `features/premium.md`;
  removed the separate `features/subscription.md` contract.
- Removed application-backend verify/status/restore dependencies from payment guidance and ads
  eligibility. The selected billing provider owns supported purchase/entitlement/restore mechanics;
  exact SDK compatibility, identity/cache policy and new UX require integration decisions.
- Updated UI, media, ads, app update, analytics and release contracts without claiming optional
  components or old pipelines are implemented. Rebuilt the index around existing documents.
- Replaced imported product history with this base-only log. Historical shipped behavior and test
  claims from the previous product are not evidence for this repository.
- Verification: documentation link and stale-reference static checks only; no application source,
  SDK artifacts, runtime configuration, secrets, tests or external services changed.
