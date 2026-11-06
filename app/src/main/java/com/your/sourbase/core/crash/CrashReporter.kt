package com.your.sourbase.core.crash
/** Optional diagnostics boundary. Expected validation/storage failures are not crash reports. */
interface CrashReporter { fun unexpectedFailure(failure: Throwable) }
