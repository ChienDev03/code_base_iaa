package com.your.sourbase.core.base
import com.your.sourbase.core.network.safeRequest
/** Reusable repository request boundary without proprietary envelopes. */
abstract class AppRepository {
    protected suspend fun <T> request(block: suspend () -> T) = safeRequest(block)
}
