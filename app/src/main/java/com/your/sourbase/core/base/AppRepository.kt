package com.your.sourbase.core.base
import com.your.sourbase.core.data.localOperation
/** Safe boundary for local reads and writes. */
abstract class AppRepository {
    protected suspend fun <T> local(block: suspend () -> T) = localOperation(block)
}
