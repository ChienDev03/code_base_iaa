package com.your.sourbase

import com.your.sourbase.core.data.DataResult
import com.your.sourbase.core.data.localOperation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Failures cannot expose database diagnostics or user content to presentation. */
class LocalOperationTest {
    @Test fun failureHasNoDiagnosticPayload() = runTest {
        assertEquals(DataResult.Failure, localOperation { error("private stored value") })
    }
}
