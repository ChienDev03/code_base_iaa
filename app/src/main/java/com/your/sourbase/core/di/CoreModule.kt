package com.your.sourbase.core.di
import com.your.sourbase.core.time.SystemTimeProvider
import com.your.sourbase.core.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
/** Infrastructure clock binding. */
@Module @InstallIn(SingletonComponent::class)
abstract class CoreModule {
    @Binds abstract fun time(implementation: SystemTimeProvider): TimeProvider
}
