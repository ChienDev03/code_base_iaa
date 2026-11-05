# Base knowledge base

Read this index, then only the owning topic. User instructions and repository instructions govern
execution. These documents define reusable integration guidance; they do not establish that an
optional feature, SDK, endpoint, screen or pipeline is implemented in this checkout.

## Find the owning contract

| Topic | Contract |
| --- | --- |
| Shared UI, tokens, state and interaction | [Design system](design-system.md) |
| Connectivity, device time, API errors and recovery | [Core modules](core_modules_specification.md) |
| Image preparation, upload and cancellation | [Image upload](image-upload.md) |
| Paywall, premium entitlement, purchase, restore and subscription details | [Premium and subscriptions](features/premium.md) |
| Ad configuration, consent and lifecycle | [AdsKit](features/adskit.md) |
| Recommended and required app updates | [App update](features/app_update_specification.md) |
| Google Play rating/review, session counting and trigger coordination | [Rating and review](features/app_rating_review_specification.md) |
| Event approval, attribution and privacy | [Event tracking](features/event_tracking.md) |
| Build, distribution and external handoff | [Release delivery](release-delivery.md) |
| Documentation changes | [Changelog](CHANGELOG.md) |

## Reading examples

Contracts retain integration maps, code snippets, JSON payloads and a few example fields so a
reader can follow the implementation flow. `example` names and placeholders replace old product
identifiers; SDK reference APIs must be checked against the chosen artifact. Examples are not
claims that optional integrations are already installed.

Imported user-provided specifications retain their source language and detailed code. New
repository adaptation notes are English and identify differences from implementation-ready rules.

## Maintenance

- Keep source and authored technical documentation in English. Use neutral examples and placeholders;
  do not copy production identifiers, device identifiers, credentials or old product requirements.
- Check source before claiming implementation, API availability, SDK versions or verification.
  Do not invent source links or retain links to absent feature contracts.
- Update the owning contract and changelog together for significant changes. Preserve skill aliases.
- Confirm new first-load, refresh, paging, action, retry and offline UX before implementation.
- Optional integrations must be absent from disabled dependency graphs and merged manifests.
- Unit, instrumented, Maestro and benchmark runs require explicit confirmation. Do not represent
  source review or static checks as runtime verification.
