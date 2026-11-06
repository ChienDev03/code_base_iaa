package com.your.sourbase.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.your.sourbase.core.database.MoneyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** One private on-device database; missing migrations fail instead of deleting user data. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): MoneyDatabase =
        Room.databaseBuilder(context, MoneyDatabase::class.java, "money_tracker.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
}
