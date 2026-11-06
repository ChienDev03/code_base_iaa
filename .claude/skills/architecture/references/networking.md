# Local operation boundary (legacy reference filename)

There is no application HTTP/API layer. See .ai-kb/core_modules_specification.md and local-storage.md.
AppRepository.local returns DataResult.Success or DataResult.Failure without diagnostic text.
Cancellation propagates. Room transactions own atomic reads/writes; injected dispatchers support
other local IO. UI resolves feature then generic resources. No network/server-time blockers.
Only the selected Ads/Firebase SDKs may perform their own service networking.
