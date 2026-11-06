package com.your.sourbase.core.services

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.your.sourbase.core.analytics.AnalyticsProvider
import com.your.sourbase.core.analytics.TelemetryControl
import com.your.sourbase.core.config.RemoteDefaults
import com.your.sourbase.core.config.RemoteSettings
import com.your.sourbase.core.crash.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase adapter, compiled only into explicitly enabled builds. No ledger dependency exists. */
@Singleton
class AppServices @Inject constructor(@ApplicationContext private val context: Context) :
    AnalyticsProvider, CrashReporter, RemoteSettings, TelemetryControl {
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }
    private val crash by lazy { FirebaseCrashlytics.getInstance() }
    private val remote by lazy { FirebaseRemoteConfig.getInstance() }

    override fun approvedEvent(name: String, parameters: Map<String, String>) {
        // Callers must use approved feature-owned event contracts; there are none in the starter.
        analytics.logEvent(name, Bundle().apply { parameters.forEach { (key, value) -> putString(key, value) } })
    }
    override fun unexpectedFailure(failure: Throwable) {
        // Strip messages/causes that could contain SQL, notes or amounts from explicit reports.
        val sanitized = RuntimeException("Unexpected application failure")
        sanitized.stackTrace = failure.stackTrace
        crash.recordException(sanitized)
    }
    override fun string(key: String): String =
        remote.getString(key).ifEmpty { RemoteDefaults.values[key].orEmpty() }

    override suspend fun refresh(): Boolean = try {
        remote.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .setFetchTimeoutInSeconds(10)
                .build(),
        ).await()
        remote.setDefaultsAsync(RemoteDefaults.values).await()
        remote.fetchAndActivate().await()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
    override fun setAnalyticsEnabled(enabled: Boolean) = analytics.setAnalyticsCollectionEnabled(enabled)
    override fun setCrashReportingEnabled(enabled: Boolean) = crash.setCrashlyticsCollectionEnabled(enabled)
}
