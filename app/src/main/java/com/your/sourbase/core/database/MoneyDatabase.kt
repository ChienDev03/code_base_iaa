package com.your.sourbase.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** First app-owned schema. Future versions require explicit, data-preserving migrations. */
@Database(
    entities = [CategoryEntity::class, BudgetEntity::class, TransactionEntity::class],
    views = [BudgetSummary::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MoneyConverters::class)
abstract class MoneyDatabase : RoomDatabase() {
    /** Local ledger queries; never accessed from the main thread. */
    abstract fun moneyDao(): MoneyDao
}
