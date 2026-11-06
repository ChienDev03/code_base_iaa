# Testing

Compile/build/lint/static checks are allowed. Unit, instrumented, Maestro and benchmark execution
require explicit user authorization per AGENTS.md. Compile-only checks do not execute tests.

Use hand-rolled fakes, not mocking libraries. Use coroutine-test dispatchers and reset Main after
ViewModel tests. Signal coroutine subscription readiness explicitly instead of arbitrary delays.
Verify cancellation, in-flight ownership, safe failures, database persistence and data-preserving
migrations. Device tests can exercise Room without mocking Android framework or vendor SDK APIs.
SDK delivery and real ads require configured devices; compile checks never establish these outcomes.

Commands after authorization:
- ./gradlew.bat testDevDebugUnitTest
- ./gradlew.bat connectedDevDebugAndroidTest

For new UI flows, offer an optional Maestro draft before creating one.
