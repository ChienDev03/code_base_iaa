# Local app knowledge base

Read this index, then the owning contract. User instructions govern execution.
Reference contracts do not imply installed SDKs or implemented screens.

| Topic | Contract |
| --- | --- |
| UI, tokens, interaction | [Design system](design-system.md) |
| Local operations, cancellation, time, errors | [Core modules](core_modules_specification.md) |
| Room, SQL, migration and data ownership | [Local storage](local-storage.md) |
| Firebase services and build selection | [Firebase](firebase.md) |
| Ads configuration, consent and lifecycle | [AdsKit](features/adskit.md) |
| App update reference | [App update](features/app_update_specification.md) |
| Play rating/review reference | [Rating and review](features/app_rating_review_specification.md) |
| Event approval and privacy | [Event tracking](features/event_tracking.md) |
| Existing build/distribution tools | [Release delivery](release-delivery.md) |
| Documentation history | [Changelog](CHANGELOG.md) |
| Local-base verification | [Checks and limitations](local-base-verification.md) |

Keep code and new technical documentation in English. Update owning contracts and changelog
together. Preserve canonical skills and their .agents aliases. Check source before claiming
implementation or runtime verification. Confirm new UX before implementing it.
Disabled SDKs must be absent from dependencies and manifests. No financial data is uploaded.
