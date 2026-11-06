package com.your.sourbase.core.di
import com.your.sourbase.features.example.data.DefaultExampleRepository
import com.your.sourbase.features.example.data.ExampleRepository
import com.your.sourbase.features.ledger.data.LedgerRepository
import com.your.sourbase.features.ledger.data.RoomLedgerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
/** Composition-root exception: binds feature repository contracts. */
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun example(implementation: DefaultExampleRepository): ExampleRepository
    @Binds @Singleton abstract fun ledger(implementation: RoomLedgerRepository): LedgerRepository
}
