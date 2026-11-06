package com.your.sourbase.core.data

import kotlinx.coroutines.CancellationException

/** Local operation outcome; diagnostics and stored values never cross the UI error boundary. */
sealed interface DataResult<out T> {
    /** Completed local operation. */
    data class Success<T>(val data: T) : DataResult<T>
    /** Presentation resolves its own localized fallback message. */
    data object Failure : DataResult<Nothing>
}

/** Converts local failures to safe outcomes while preserving structured cancellation. */
suspend fun <T> localOperation(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    DataResult.Failure
}
