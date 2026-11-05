package com.example.base.core.crash
/** Optional diagnostics boundary. Ordinary API failures must not be automatically reported. */
interface CrashReporter { fun unexpectedFailure(failure: Throwable) }
