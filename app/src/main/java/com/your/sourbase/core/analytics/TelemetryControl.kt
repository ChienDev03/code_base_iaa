package com.your.sourbase.core.analytics

/** Explicit opt-in controls, independent of ads consent and local database access. */
interface TelemetryControl {
    /** Applies the product's explicit Analytics collection choice. */
    fun setAnalyticsEnabled(enabled: Boolean)
    /** Applies the product's explicit diagnostic collection choice. */
    fun setCrashReportingEnabled(enabled: Boolean)
}
