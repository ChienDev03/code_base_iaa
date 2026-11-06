package com.your.sourbase.features.ledger.data

import androidx.room.withTransaction
import com.your.sourbase.core.base.AppRepository
import com.your.sourbase.core.data.DataResult
import com.your.sourbase.core.database.BudgetEntity
import com.your.sourbase.core.database.CategoryEntity
import com.your.sourbase.core.database.MoneyDatabase
import com.your.sourbase.core.database.MoneySnapshot
import com.your.sourbase.core.database.TransactionEntity
import com.your.sourbase.core.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Local ledger boundary; financial values never leave the device. */
interface LedgerRepository {
    /** Reads a consistent local snapshot. */
    suspend fun load(): DataResult<MoneySnapshot>
    /** Saves a named category and returns its stable ID. */
    suspend fun saveCategory(value: CategoryEntity): DataResult<Long>
    /** Saves a nonnegative budget without invalidating attached transaction currencies. */
    suspend fun saveBudget(value: BudgetEntity): DataResult<Long>
    /** Saves a validated transaction and stamps its update time. */
    suspend fun saveTransaction(value: TransactionEntity): DataResult<Unit>
    /** Deletes a transaction by ID. */
    suspend fun deleteTransaction(id: String): DataResult<Unit>
    /** Deletes a budget while preserving its transaction history. */
    suspend fun deleteBudget(id: Long): DataResult<Unit>
    /** Deletes a category while preserving its transaction history. */
    suspend fun deleteCategory(id: Long): DataResult<Unit>
}

/** Validates local writes and uses transactions for currency checks plus persistence. */
@Singleton
class RoomLedgerRepository @Inject constructor(
    private val database: MoneyDatabase,
    private val time: TimeProvider,
) : AppRepository(), LedgerRepository {
    private val dao get() = database.moneyDao()
    override suspend fun load() = local { dao.snapshot() }
    override suspend fun saveCategory(value: CategoryEntity) = local {
        require(value.name.isNotBlank())
        dao.saveCategory(value)
    }
    override suspend fun saveBudget(value: BudgetEntity) = local {
        require(value.name.isNotBlank() && value.totalMinor >= 0 && validCurrency(value.currency))
        database.withTransaction {
            require(dao.differentCurrencyCount(value.id, value.currency) == 0)
            dao.saveBudget(value.copy(updatedAt = time.epochMillis() / 1000))
        }
    }
    override suspend fun saveTransaction(value: TransactionEntity) = local {
        require(value.id.isNotBlank() && value.amountMinor >= 0 && validCurrency(value.currency))
        database.withTransaction {
            value.budgetId?.let { require(dao.budget(it)?.currency == value.currency) }
            dao.saveTransaction(value.copy(updatedAt = time.epochMillis() / 1000))
        }
    }
    override suspend fun deleteTransaction(id: String) = local { dao.deleteTransaction(id) }
    override suspend fun deleteBudget(id: Long) = local { dao.deleteBudget(id) }
    override suspend fun deleteCategory(id: Long) = local { dao.deleteCategory(id) }
    private fun validCurrency(value: String) = value.length == 3 && value.all { it in 'A'..'Z' }
}
