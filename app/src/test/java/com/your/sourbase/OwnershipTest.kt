package com.your.sourbase
import androidx.navigation3.runtime.NavKey
import com.your.sourbase.navigation.AppRouteSerializersModule
import com.your.sourbase.features.example.presentation.ExampleDetail
import com.your.sourbase.core.config.IntegrationState
import com.your.sourbase.core.network.safeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test
/** Durable route and cancellation boundaries independent of device lifecycle. */
class OwnershipTest {
    @Test fun typedRouteRoundTripsWithRegistry() {
        val json = Json { serializersModule = AppRouteSerializersModule }
        val route: NavKey = ExampleDetail(12)
        assertEquals(route, json.decodeFromString<NavKey>(json.encodeToString<NavKey>(route)))
    }
    @Test fun consentAloneDoesNotEnableIntegration() {
        assertFalse(IntegrationState(consentGranted = true).available)
        assertTrue(IntegrationState(true, true, true, true, true).available)
    }
    @Test fun requestCancellationIsNotApiFailure() = runTest {
        try {
            safeRequest<Nothing> { throw CancellationException("cancelled") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { /* expected */ }
    }
}
