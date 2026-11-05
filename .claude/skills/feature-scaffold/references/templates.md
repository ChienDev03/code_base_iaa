# Feature vertical templates

Copy-ready skeletons. Replace `<Name>` (PascalCase) / `<name>` (lowercase package) throughout.
These mirror `features/home` — read that vertical when a detail here is ambiguous.

---

## `data/repository/<Name>Repository.kt`

```kotlin
package com.your.sourbase.features.<name>.data.repository

import com.your.sourbase.features.<name>.data.model.<Name>Snapshot

/** Source of the data needed by the <Name> destination. */
interface <Name>Repository {
    /** Loads the latest <Name> snapshot or throws when it cannot be loaded. */
    suspend fun load<Name>(): <Name>Snapshot
}
```

## `data/repository/<Name>RepositoryImpl.kt`

Plain implementation (no network yet — name it `InMemory<Name>Repository` while the data is faked):

```kotlin
package com.your.sourbase.features.<name>.data.repository

import com.your.sourbase.core.time.TimeProvider
import com.your.sourbase.features.<name>.data.model.<Name>Snapshot
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory <Name> source used until the real API arrives. */
@Singleton
class InMemory<Name>Repository @Inject constructor(
    private val timeProvider: TimeProvider,
) : <Name>Repository {
    // TODO: replace with the real data source once the API lands.
    override suspend fun load<Name>(): <Name>Snapshot = <Name>Snapshot(/* ... */)
}
```

Network-backed variant — extend `core/base/AppRepository` and return `ApiResult`:

```kotlin
@Singleton
class <Name>RepositoryImpl @Inject constructor(
    private val api: <Name>Api,
) : AppRepository(), <Name>Repository {
    override suspend fun load<Name>(): ApiResult<<Name>Snapshot> = safeApiCall { api.fetch<Name>() }
}
```

---

## `presentation/<Name>Contract.kt`

```kotlin
package com.your.sourbase.features.<name>.presentation

import com.your.sourbase.core.base.UiEvent
import com.your.sourbase.core.base.UiState
import com.your.sourbase.core.ui.snackbar.UiText

/** Immutable state rendered by the <Name> destination. */
data class <Name>UiState(
    val isLoading: Boolean = true,      // first load -> skeleton
    val isRefreshing: Boolean = false,  // pull-to-refresh -> keep content, show indicator
    val error: UiText? = null,
    // ...display-ready fields; convert Instant -> LocalDateTime in the ViewModel, not the UI
) : UiState

/** One-shot navigation effects emitted by <Name>. */
sealed interface <Name>UiEvent : UiEvent {
    /** Opens ... */
    data object OpenSomething : <Name>UiEvent
}

/** User actions consumed by stateless [<Name>Content]. */
data class <Name>Callbacks(
    val onSomethingClick: () -> Unit,
    val onRefresh: () -> Unit,
)
```

Rules: state fields are display-ready; events are navigation/one-shot effects **only** — user-facing
messages go through `SnackbarController`. UI-only models (`<X>UiModel`) and enums live here too.

---

## `presentation/<Name>ViewModel.kt`

```kotlin
package com.your.sourbase.features.<name>.presentation

import androidx.lifecycle.viewModelScope
import com.your.sourbase.R
import com.your.sourbase.core.base.AppViewModel
import com.your.sourbase.core.crashlytics.CrashReporter
import com.your.sourbase.core.logging.AppLog
import com.your.sourbase.core.ui.components.feedback.AppSnackbarType
import com.your.sourbase.core.ui.snackbar.SnackbarController
import com.your.sourbase.core.ui.snackbar.UiText
import com.your.sourbase.features.<name>.analytics.<Name>Tracker
import com.your.sourbase.features.<name>.data.repository.<Name>Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Coordinates <Name> repository data and its navigation effects. */
@HiltViewModel
class <Name>ViewModel @Inject constructor(
    private val repository: <Name>Repository,
    private val snackbar: SnackbarController,
    private val crashReporter: CrashReporter,
    private val tracker: <Name>Tracker,     // only once the feature has its first (mock) event
) : AppViewModel<<Name>UiState, <Name>UiEvent>(<Name>UiState()) {
    private val log = AppLog.tag("<Name>ViewModel")

    init {
        load()
    }

    /** Handles ... by ... */
    fun onSomethingClick() {
        log.d("onSomethingClick")
        tracker.trackSomethingClicked()
        postEvent(<Name>UiEvent.OpenSomething)
    }

    /** Pull-to-refresh: reloads without clearing the content already on screen. */
    fun onRefresh() {
        log.d("onRefresh")
        updateState { it.copy(isRefreshing = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { repository.load<Name>() }
                .onSuccess { snapshot ->
                    log.d("load success")   // log IDs/counts, never bodies or personal data
                    updateState { it.copy(isLoading = false, isRefreshing = false, error = null /* , ... */) }
                }
                .onFailure { throwable ->
                    log.d("load failed: ${throwable::class.simpleName}")
                    crashReporter.recordException(throwable)
                    // Network-backed: error.toUiText(R.string.<name>_load_error) -> backend message first.
                    updateState {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiText.Res(R.string.<name>_load_error),
                        )
                    }
                }
        }
    }
}
```

Show a message instead of an event when there is nothing to navigate to:

```kotlin
snackbar.show(text = UiText.Res(R.string.message_coming_soon), type = AppSnackbarType.Info)
```

---

## `presentation/<Name>Screen.kt`

```kotlin
/** Stateful <Name> destination that connects Hilt state and navigation. */
@Composable
fun <Name>Screen(
    onOpenSomething: () -> Unit,
    viewModel: <Name>ViewModel = hiltViewModel(),   // hiltViewModel(), never viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle, onOpenSomething) {
        viewModel.events
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    <Name>UiEvent.OpenSomething -> onOpenSomething()
                }
            }
    }
    <Name>Content(
        state = state,
        callbacks = <Name>Callbacks(
            onSomethingClick = viewModel::onSomethingClick,
            onRefresh = viewModel::onRefresh,
        ),
    )
}

/** Stateless <Name> content used by production, previews, and instrumentation tests. */
@Composable
fun <Name>Content(
    state: <Name>UiState,
    callbacks: <Name>Callbacks,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        <Name>LoadingContent(modifier)   // skeleton blocks: Modifier.clip(AppShapes.radius12).shimmer()
        return
    }
    AppPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = callbacks.onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = null,   // required on every scrollable
        ) {
            // token-driven content — see the compose-design-system skill.
            // Raw .clickable { } must use rememberDebouncedClick(...); core/ui components debounce already.
        }
    }
}

@AppPreviews
@Composable
private fun <Name>ContentPreview() {
    AppTheme {
        <Name>Content(state = <Name>UiState(isLoading = false), callbacks = <Name>Callbacks(onSomethingClick = {}, onRefresh = {}))
    }
}
```

---

## `analytics/<Name>Tracker.kt` (mock — only once the feature has an event)

```kotlin
package com.your.sourbase.features.<name>.analytics

import com.your.sourbase.core.logging.AppLog
import javax.inject.Inject

/** Feature-level analytics for <Name>. Mocked until the official tracking spec exists. */
class <Name>Tracker @Inject constructor() {
    private val log = AppLog.tag("<Name>Tracker")

    /** Reports that the user tapped something. */
    fun trackSomethingClicked() {
        // TODO(tracking-spec): wire once the official event is defined
        log.d("[mock] $EVENT_SOMETHING_CLICKED")
    }

    private companion object {
        const val EVENT_SOMETHING_CLICKED = "mock_something_clicked"
    }
}
```

Never inject `core/analytics/Tracker` or `AttributionTracker` here or anywhere in the feature until
the user supplies the tracking spec (architecture skill → `references/analytics-and-consent.md`).
In unit tests, construct the real `<Name>Tracker()` — it has no dependencies.

---

## Wiring diffs

`core/di/RepositoryModule.kt`:

```kotlin
    /** Binds the <Name> feature repository. */
    @Binds
    abstract fun bind<Name>Repository(impl: InMemory<Name>Repository): <Name>Repository
```

`features/<name>/presentation/<Name>Navigation.kt` (new per-feature file; routes + serializer module + entry DSL all live here):

```kotlin
package com.your.sourbase.features.<name>.presentation
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Navigation key for the <Name> destination. */
@Serializable data object <Name>Route : NavKey

/** Polymorphic registrations for this feature. Add every @Serializable route above. */
val <Name>RouteSerializers: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(<Name>Route::class, <Name>Route.serializer())
    }
}

/** Entries owned by this feature. AppNavGraph calls this once; no inline entry<> elsewhere. */
fun EntryProviderScope<NavKey>.<name>Entries(stack: NavBackStack<NavKey>) {
    entry<<Name>Route> {
        <Name>Screen(onOpenSomething = { stack.add(SomethingRoute) })
    }
}
```

`navigation/Routes.kt` — chain the feature's serializer module into the composed `AppRouteSerializersModule`:

```kotlin
val AppRouteSerializersModule: SerializersModule =
    ExampleRouteSerializers + <Name>RouteSerializers // + more features...
```

`navigation/AppNavGraph.kt`, inside `entryProvider { }` — one line per feature:

```kotlin
                <name>Entries(stack)
```

---

## `app/src/test/.../<Name>ViewModelTest.kt`

Hand-rolled fakes only — no Mockito/MockK anywhere in this repo.

```kotlin
/** Tests <Name> UDF state loading and one-shot events. */
@OptIn(ExperimentalCoroutinesApi::class)
class <Name>ViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    /** Installs a deterministic main dispatcher for ViewModel coroutines. */
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    /** Restores the platform main dispatcher after each test. */
    @After fun tearDown() { Dispatchers.resetMain() }

    /** Loads data from the repository. */
    @Test
    fun initialLoadPublishesRepositorySnapshot() = runTest(dispatcher) {
        val snapshot = <Name>Snapshot(/* ... */)
        val viewModel = <Name>ViewModel(Fake<Name>Repository(snapshot), SnackbarController())

        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    /** Emits the navigation effect exactly once. */
    @Test
    fun somethingClickEmitsOpenEvent() = runTest(dispatcher) {
        val viewModel = <Name>ViewModel(Fake<Name>Repository(<Name>Snapshot()), SnackbarController())
        val events = mutableListOf<<Name>UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onSomethingClick()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(<Name>UiEvent.OpenSomething), events)
        job.cancel()
    }

    private class Fake<Name>Repository(private val snapshot: <Name>Snapshot) : <Name>Repository {
        override suspend fun load<Name>(): <Name>Snapshot = snapshot
    }

    private fun timeAt(iso: String) = object : TimeProvider {
        override fun now(): Instant = Instant.parse(iso)
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }
}
```

## `app/src/androidTest/.../<Name>ScreenTest.kt`

```kotlin
/** Instrumentation coverage for stateless <Name> content and callback wiring. */
class <Name>ScreenTest {
    @get:Rule val composeRule = createComposeRule()

    /** Renders content and dispatches the primary callback. */
    @Test
    fun somethingRendersAndDispatchesClick() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                <Name>Content(
                    state = <Name>UiState(isLoading = false),
                    callbacks = <Name>Callbacks(onSomethingClick = { clicks++ }),
                )
            }
        }

        composeRule.onNodeWithText("...").assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }
}
```
