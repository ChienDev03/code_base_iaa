# Testing conventions

## Running tests — manual, confirm with the user first

Never run unit, instrumented, or Maestro tests on your own initiative. Write or update the tests,
then list the exact commands and **ask the user** whether to run them now or run them manually.
`.claude/settings.json` puts `./gradlew test*`, `connected*AndroidTest`, and `maestro test` behind an
`ask` prompt to enforce this. A compile check (`./gradlew compileDevDebugKotlin`) is allowed without
asking.

After a UI feature or a user-facing flow change, **offer** (optional) to draft a Maestro flow under
`.maestro/<feature>.yaml` for that flow — propose it, don't create it unasked. The `.maestro/`
directory does not exist yet; the first accepted flow creates it.

## No mocking library — ever

There is no Mockito/MockK in this project. Every test uses a **hand-rolled fake class implementing
the real interface**. Use the current Home repository/tracker fakes in `app/src/test/` as examples;
inspect the actual interface before copying a fake from an older test.
Keep following this convention; do not introduce a mocking dependency.

Fakes are usually `private` classes nested at the bottom of the test file that needs them.

## Coroutines

`kotlinx-coroutines-test` is a `testImplementation` dependency. Standard shape for a ViewModel test:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class XViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun x() = runTest(dispatcher) {
        val viewModel = XViewModel(FakeRepository(), SnackbarController())
        testScheduler.advanceUntilIdle()
    }
}
```

Collect one-shot `events` with `launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }`
and `job.cancel()` at the end. Inject a fake `TimeProvider` rather than depending on the device clock.

## DataStore in a plain JVM test

`PreferenceDataStoreFactory.create(produceFile = ...)` works in a plain JVM unit test — no
Robolectric, no `Context` — by pointing `produceFile` at a real temp file. See
`DataStoreOnboardingRepositoryTest` in `core/datastore` for a current example.

## Hot-flow race avoidance

When a repository under test starts collecting a hot `Flow`/`SharedFlow` asynchronously, signal collection start
from the fake and await it before emitting:

```kotlin
// in the fake
override val purchaseUpdates = flow.onStart { collectionStarted.complete(Unit) }
// in the test
awaitCollection()
emit(...)
```

**Do not rely on `yield()` / `delay()` alone** to sequence against a hot flow's subscriber.

## Accepted coverage gaps

Static third-party SDK calls (Firebase, Clarity, Adjust, and Play Billing types like `ProductDetails`
/ `Purchase`) cannot be invoked or constructed in a plain JVM unit test without a mocking library
this project doesn't have.

Inspect the current test file before claiming a path is covered. Historical consent guards and
app-owned BillingClient wrappers have been removed; do not copy their former coverage claims.
PaymentKit owns billing/Adjust. Exercise SDK boundaries through the supported seams or Android
environment when authorized, rather than invoking static third-party APIs in a plain JVM test.

Don't treat these as missing work to be filled in with a new dependency.

## Instrumented and E2E

- `app/src/androidTest/` — `createComposeRule()`, render the **stateless** `XContent` (or the shared
  component) inside `AppTheme`, assert on text / content description and callback dispatch.
- `.maestro/*.yaml` — E2E flows (none committed yet) run against an installed `devDebug` build. They
  select on visible text and accessibility labels, so renaming those breaks E2E.

```bash
# Only after the user confirms:
./gradlew testDevDebugUnitTest
./gradlew connectedDevDebugAndroidTest     # needs a device/emulator
maestro test .maestro/
```
