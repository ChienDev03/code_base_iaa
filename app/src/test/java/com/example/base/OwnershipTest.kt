package com.example.base
import androidx.navigation3.runtime.NavKey
import com.example.base.navigation.AppRouteSerializersModule
import com.example.base.navigation.ExampleDetail
import com.example.base.core.config.IntegrationState
import com.example.base.core.network.safeRequest
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
