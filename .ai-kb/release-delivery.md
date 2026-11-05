# Release delivery

Reusable release guidance. No CI provider, branch convention, distribution destination, project ID,
external board or notification channel is preselected. Check the current pipeline before claiming
any delivery capability is implemented.

## Build and configuration

Use the Gradle wrapper. Define variants, version identity, signing inputs and the supported toolchain
in the consuming app's release configuration. Keep credentials and signing material outside source
control; document variable names and templates without copying values or reading secret files.

Bound CI memory and worker concurrency for the actual runner. Cache compatible dependencies and
wrapper distributions with toolchain/dependency-aware keys. Do not cache secrets or assume a
missing daemon proves OOM. Inspect build evidence before changing memory settings.

Optional SDKs must be absent from disabled dependency graphs and merged manifests. Verify enabled
and disabled release variants with appropriate build/static checks. Preserve useful mapping files
and verify serializers/reflection requirements when minification is enabled.

## Starter debug configuration

The `environment` dimension provides `dev` and `production`; debug and release produce four variants.
`devDebug` / `productionDebug` are debuggable with default Android debug signing. Dev adds `.dev`,
`-dev` and a `[DEV]` app label; `APP_NAME` can override either flavor label. Release remains unsigned.
The wrapper pins Gradle 9.4.1 and daemon JDK 25. Android Studio may launch it with its bundled JDK;
`scripts/gradle.sh` provides the same macOS fallback when terminal JAVA_HOME is unset.
Open the root project, sync, select the app's debug variant and shared `app` run configuration.
No Firebase configuration, private SDK or signing material is required for the offline starter.

### IDE source downloads and macOS folders

Android Studio can attribute parent-module sample files to the resolved `-android` component.
Checksums must also exist under that exact component coordinate. The 24 sample aliases reported
on 2026-11-05 were verified against their published parent-module Google Maven SHA-1 sidecars
and pinned with SHA-256 under the Android components; parent-only coverage is insufficient.
A later runtime-classpath report required six more aliases: Compose UI 1.11.2 (ui, graphics,
test, text, unit) and window-core 1.5.0. These received the same official-sidecar verification;
compile-classpath checks alone do not cover IDE runtime requests.
The unit-test runtime classpath additionally requested ui-tooling and ui-tooling-preview 1.11.2
sample aliases; both were verified and pinned under their Android component coordinates.

Keep checksums for IDE-requested source artifacts as well as build binaries. A successful assemble
alone does not verify Android Studio's source downloads. On 2026-11-05, 111 missing plugin-source
checksums were added after comparing cached files with Google Maven / Maven Central SHA-1 sidecars;
SHA-256 entries are retained in verification-metadata.xml. Verification remains enabled.
A subsequent IDE sync also requested five Kotlin `gradle813-sources.jar` variants; their checksums
were verified against Maven Central and added separately. Source classifiers vary by Gradle plugin
variant, so checking only the plain `sources` classifier does not cover every IDE request.
IDE sync also resolves application/test source and Javadoc artifacts. The final documentation
resolution check covered every resolvable compile/runtime classpath: 229 artifacts from 202
components passed strict verification after 226 additional checksum entries were compared with
official repository sidecars. This checks dependency resolution, not the IDE UI or a device run.
IDE sync also requests Javadoc for the root plugin classpath (AGP, Kotlin, KSP, Hilt plugin and
their transitive tools); 85 such checksums were added after sidecar comparison. Run the check for
root and `:app` in one Gradle invocation: a docs-verification failure in one project surfaces only
in a later resolution, so per-project runs can falsely pass. A failed sync leaves Android Studio
with no Build Variants; the CLI build still succeeds because it never fetches docs artifacts.
A Gradle `sources` variant can list several files: AndroidX also publishes `-samples-sources.jar`
and KMP roots `-multiplatform-sources.jar`. Cover every file listed by each pinned component's
`.module` documentation variants, not only classifiers seen in one sync. On 2026-11-05 a full
scan added 311 such entries (sidecar-verified) and left zero published documentation files
without a checksum.

The macOS protected-folder warning is independent of checksum failures. Prefer a development
folder such as `~/Developer/base-source` when choosing a new location. For a project kept under
Documents, allow Android Studio access through System Settings > Privacy & Security > Files and
Folders when requested. Do not disable OS protection or use broad permission changes.

## Distribution and external handoff

Separate build, upload, ticket changes and notifications. A failed build/upload must not advance
external state. Track release/artifact identity, apply idempotent checkpoints and reconcile ambiguous
writes before retrying. Prevent older releases or replay from regressing terminal/newer state.

Destination IDs, ticket associations, schema labels, allowlists and retry/retention rules require
explicit configuration. Keep local journals in ignored storage with bounded retention. A dry-run
must perform zero external writes; a successful dry-run is not evidence of live adapter behavior.

Do not publish, commit, change live status or send messages without authorization. Capabilities and
configured credentials do not establish authorization. Never infer recipients or destinations from
an old project.

## Verification

Report actual compile/build/lint/static results and any unverified runtime or live integration.
Unit, instrumented, Maestro and benchmark runs require explicit confirmation. External delivery
checks and mutations require their own authorization; documentation does not establish success.

## Reference pipeline and configuration fields

The imported integration used the following understandable separation. Providers are examples,
not configured destinations or authorization to run them:

```text
CI (for example CircleCI)
→ Fastlane build lane
→ artifact distribution (for example Firebase App Distribution)
→ optional release-board adapter
→ optional notification adapter
```

Keep build/upload evidence separate from external handoff state. Do not describe a Notion/Discord
adapter or branch-trigger convention as implemented unless it exists in the current checkout.

| Example input | Purpose |
| --- | --- |
| `releaseVersionCode` Gradle property | Inject the selected monotonically increasing build identity |
| `ENV_FILE_BASE64_DEV` | CI-provided development environment bundle |
| `ENV_FILE_BASE64_PRODUCTION` | CI-provided production environment bundle |
| `FIREBASE_APP_ID` | Configured example distribution target |
| `FIREBASE_TESTER_GROUPS` | Approved distribution audience |

These are variable names only, not secret values. Use the authentication mechanism supported by
the actual release tooling; do not revive an old token configuration by copying this table.

Reference build invocation, using a configured variant and version code:

```sh
./gradlew :app:bundleProductionRelease -PreleaseVersionCode=123
```

The task is an example, not a command run by this documentation change. Match the actual variant
and obtain publication authorization separately; building does not authorize uploading.

For CI memory planning, the imported sample used a 4 GB Gradle heap, 1 GB Kotlin daemon heap,
two workers and parallel execution disabled on an 8 GB runner. Treat these as sizing examples:
heap excludes native/other process memory. Cache wrapper/dependency data with image, architecture,
wrapper and dependency-catalog checksums; exclude signing files and environment bundles.
