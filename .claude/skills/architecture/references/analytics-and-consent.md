# Analytics and consent

See .ai-kb/features/event_tracking.md and .ai-kb/firebase.md.
There is no approved event catalog, automatic navigation tracker or attribution SDK.
AnalyticsProvider and CrashReporter bind to build-selected AppServices.
Firebase collection defaults off; TelemetryControl supports explicit opt-in independently for each.
Unapproved events stay unconnected. Never include ledger values or notes in diagnostics or events.
Ads consent is a separate lifecycle requirement and does not enable telemetry implicitly.
