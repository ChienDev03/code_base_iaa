package com.your.sourbase
import com.your.sourbase.core.network.ApiResult
import com.your.sourbase.core.network.ApiError
import com.your.sourbase.core.ui.SnackbarController
import com.your.sourbase.features.example.data.*
import com.your.sourbase.features.example.presentation.ExampleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
/** Behavioral regression checks using a hand-rolled repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleViewModelTest {
    @Test fun repeatedRefreshPreservesDataAndHasOneOwner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake = FakeRepository()
            val model = ExampleViewModel(fake, SnackbarController())
            model.reload()
            runCurrent()
            assertEquals(1, fake.calls)
            fake.response.complete(ApiResult.Success(ExampleSnapshot(listOf(Example(1, null, null)), 1)))
            runCurrent()
            assertEquals(1, model.state.value.items.size)
            fake.response = CompletableDeferred()
            model.reload(); model.reload()
            runCurrent()
            assertEquals(2, fake.calls)
            assertTrue(model.state.value.refreshing)
            assertEquals(1, model.state.value.items.size)
            fake.response.complete(ApiResult.Failure(ApiError()))
            runCurrent()
            assertFalse(model.state.value.refreshing)
            assertEquals(1, model.state.value.items.size)
        } finally { Dispatchers.resetMain() }
    }
    private class FakeRepository : ExampleRepository {
        var calls = 0
        var response = CompletableDeferred<ApiResult<ExampleSnapshot>>()
        override suspend fun load(): ApiResult<ExampleSnapshot> { calls++; return response.await() }
    }
}
