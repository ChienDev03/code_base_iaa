package com.your.sourbase.core.services

import com.your.sourbase.core.analytics.AnalyticsProvider
import com.your.sourbase.core.analytics.TelemetryControl
import com.your.sourbase.core.config.RemoteDefaults
import com.your.sourbase.core.config.RemoteSettings
import com.your.sourbase.core.crash.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/** SDK-free implementation selected when enableFirebase=false. */
@Singleton
class AppServices @Inject constructor() : AnalyticsProvider, CrashReporter, RemoteSettings, TelemetryControl {
    override fun approvedEvent(name: String, parameters: Map<String, String>) = Unit
    override fun unexpectedFailure(failure: Throwable) = Unit
    override fun string(key: String) = RemoteDefaults.values[key].orEmpty()
    override suspend fun refresh() = false
    override fun setAnalyticsEnabled(enabled: Boolean) = Unit
    override fun setCrashReportingEnabled(enabled: Boolean) = Unit
}
