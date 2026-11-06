# Firebase integration

Selected services: Analytics, Crashlytics, Remote Config. No Auth, Firestore, Realtime Database,
Storage, Messaging, billing or financial-data synchronization is added.

## Build selection

`-PenableFirebase=false` (default) selects src/local/java: no-op analytics/crash controls and
bundled RemoteSettings defaults. No Firebase runtime dependencies, providers or metadata are added.

`-PenableFirebase=true` selects src/firebase/java, the Firebase BoM and the three chosen SDKs,
Google Services and Crashlytics plugins, and the Firebase manifest overlay for both flavors.
Supply your own ignored app/src/dev/google-services.json and app/src/production/google-services.json
matching the app IDs. Config files and Firebase Console were not created or modified here.
Missing config intentionally prevents an enabled app build.

## Collection and configuration

TelemetryControl can enable/disable Analytics and Crashlytics separately. Collection defaults to
false in the Firebase manifest; no feature event catalog is approved. Do not automatically enable
collection from App.onCreate, ads consent, or Remote Config. SDKs persist explicit collection overrides.
Automatic screen and advertising-ID collection are disabled in the starter overlay.

CrashReporter's explicit nonfatal reporting strips exception messages/causes. Do not pass personal
data through custom keys, logs or events; Firebase's own crash collection must be considered when
the product enables collection. No repository automatically reports expected failures.

RemoteSettings.string reads activated values, with bundled defaults for missing/empty values.
refresh is explicit, bounded by SDK timeout, cancellable, and returns false on failure while keeping
cached/default values. It is never called by local database operations or used as a startup gate.
Refresh does not parse product config; the Ads/Update consumers own schema validation.
Defaults disable configured ads and forced updates.

## Verification boundary

Compile and dependency/manifest inspection do not prove event delivery, crash upload or fetch.
Those require the owner's project configuration and device verification. No Console publication,
analytics catalog, ad placement or update UI is implied by this integration.

## Primary references

- [Android Firebase setup](https://firebase.google.com/docs/android/setup)
- [Crashlytics setup](https://firebase.google.com/docs/crashlytics/android/get-started)
- [Remote Config](https://firebase.google.com/docs/remote-config/get-started?platform=android)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
