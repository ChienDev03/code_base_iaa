package com.example.base.core.base
import com.example.base.core.network.safeRequest
/** Reusable repository request boundary without proprietary envelopes. */
abstract class AppRepository {
    protected suspend fun <T> request(block: suspend () -> T) = safeRequest(block)
}
