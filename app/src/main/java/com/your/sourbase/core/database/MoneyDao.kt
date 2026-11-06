package com.your.sourbase.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

/** Atomic local snapshot consumed by repositories. */
data class MoneySnapshot(
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetSummary>,
    val transactions: List<TransactionEntity>,
)

/** Suspend-only storage access; updates preserve foreign-key relationships. */
@Dao
abstract class MoneyDao {
    /** Returns categories in stable display order. */
    @Query("SELECT * FROM categories ORDER BY name, id")
    abstract suspend fun categories(): List<CategoryEntity>
    /** Computes current expense totals from persisted transactions. */
    @Query("SELECT * FROM budget_summary ORDER BY name, id")
    abstract suspend fun budgets(): List<BudgetSummary>
    /** Returns the ledger newest first, with stable ordering for matching timestamps. */
    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, id")
    abstract suspend fun transactions(): List<TransactionEntity>
    /** Looks up the budget used for transactional currency validation. */
    @Query("SELECT * FROM budgets WHERE id = :id")
    abstract suspend fun budget(id: Long): BudgetEntity?
    /** Detects existing rows that would conflict with a budget currency change. */
    @Query("SELECT COUNT(*) FROM transactions WHERE budget_id = :id AND currency != :currency")
    abstract suspend fun differentCurrencyCount(id: Long, currency: String): Int

    @Insert protected abstract suspend fun insertCategory(value: CategoryEntity): Long
    @Update protected abstract suspend fun updateCategory(value: CategoryEntity): Int
    @Insert protected abstract suspend fun insertBudget(value: BudgetEntity): Long
    @Update protected abstract suspend fun updateBudget(value: BudgetEntity): Int

    /** Fails duplicate names or missing update targets instead of reporting a successful no-op. */
    @Transaction
    open suspend fun saveCategory(value: CategoryEntity): Long = if (value.id == 0L) {
        insertCategory(value)
    } else {
        check(updateCategory(value) == 1)
        value.id
    }

    /** Inserts new budgets and updates existing parents without replacing their row. */
    @Transaction
    open suspend fun saveBudget(value: BudgetEntity): Long = if (value.id == 0L) {
        insertBudget(value)
    } else {
        check(updateBudget(value) == 1)
        value.id
    }

    /** Inserts or updates a ledger row by its stable identifier. */
    @Upsert abstract suspend fun saveTransaction(value: TransactionEntity)
    /** Removes one local transaction. */
    @Query("DELETE FROM transactions WHERE id = :id") abstract suspend fun deleteTransaction(id: String)
    /** Removes a budget while retaining its transactions through SET NULL. */
    @Query("DELETE FROM budgets WHERE id = :id") abstract suspend fun deleteBudget(id: Long)
    /** Removes a category while retaining its transactions through SET NULL. */
    @Query("DELETE FROM categories WHERE id = :id") abstract suspend fun deleteCategory(id: Long)

    /** Reads all tables in one SQLite transaction so totals match their ledger rows. */
    @Transaction
    open suspend fun snapshot(): MoneySnapshot = MoneySnapshot(categories(), budgets(), transactions())
}
