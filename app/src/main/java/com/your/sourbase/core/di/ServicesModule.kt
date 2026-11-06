package com.your.sourbase.core.di

import com.your.sourbase.core.analytics.AnalyticsProvider
import com.your.sourbase.core.analytics.TelemetryControl
import com.your.sourbase.core.config.RemoteSettings
import com.your.sourbase.core.crash.CrashReporter
import com.your.sourbase.core.services.AppServices
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Selects Firebase or SDK-free services at build time through the source set. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServicesModule {
    @Binds abstract fun analytics(implementation: AppServices): AnalyticsProvider
    @Binds abstract fun crash(implementation: AppServices): CrashReporter
    @Binds abstract fun remoteSettings(implementation: AppServices): RemoteSettings
    @Binds abstract fun telemetryControl(implementation: AppServices): TelemetryControl
}
