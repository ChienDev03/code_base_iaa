package com.your.sourbase

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.your.sourbase.core.data.DataResult
import com.your.sourbase.core.database.BudgetEntity
import com.your.sourbase.core.database.CategoryEntity
import com.your.sourbase.core.database.MoneyDatabase
import com.your.sourbase.core.database.TransactionEntity
import com.your.sourbase.core.database.TransactionType
import com.your.sourbase.core.time.TimeProvider
import com.your.sourbase.features.ledger.data.RoomLedgerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

/** Device checks for real SQLite persistence, currency validation and foreign-key behavior. */
class LocalLedgerTest {
    @Test fun ledgerSurvivesReopenAndParentDeletionPreservesTransactions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "ledger-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, MoneyDatabase::class.java, name).build()
        val time = object : TimeProvider {
            override fun epochMillis() = 2_000_000L
            override fun monotonicMillis() = 1L
        }
        var db = open()
        try {
            val repository = RoomLedgerRepository(db, time)
            val category = CategoryEntity(name = "Food", transactionType = TransactionType.Expense, createdAt = 1)
            val categoryId = (repository.saveCategory(category) as DataResult.Success).data
            val budget = BudgetEntity(name = "Food", totalMinor = 100_000, currency = "VND", createdAt = 1, updatedAt = 1)
            val budgetId = (repository.saveBudget(budget) as DataResult.Success).data
            assertEquals(DataResult.Failure, repository.saveCategory(category))
            assertEquals(DataResult.Failure, repository.saveBudget(budget))
            assertEquals(DataResult.Failure, repository.saveBudget(budget.copy(id = budgetId + 1)))
            val expense = TransactionEntity(
                id = "expense", amountMinor = 25_000, transactionType = TransactionType.Expense,
                currency = "VND", categoryId = categoryId, budgetId = budgetId,
                occurredAt = 1, createdAt = 1, updatedAt = 1,
            )
            assertEquals(DataResult.Success(Unit), repository.saveTransaction(expense))
            assertEquals(DataResult.Success(Unit), repository.saveTransaction(
                expense.copy(id = "income", transactionType = TransactionType.Income),
            ))
            assertEquals(DataResult.Failure, repository.saveTransaction(expense.copy(id = "bad-currency", currency = "USD")))
            assertEquals(DataResult.Failure, repository.saveTransaction(expense.copy(id = "negative", amountMinor = -1)))
            assertEquals(DataResult.Failure, repository.saveBudget(budget.copy(id = budgetId, currency = "USD")))
            assertEquals(DataResult.Success(budgetId), repository.saveBudget(budget.copy(id = budgetId, totalMinor = 120_000)))

            db.close()
            db = open()
            val stored = db.moneyDao().snapshot()
            assertEquals(2, stored.transactions.size)
            assertEquals(25_000L, stored.budgets.single().spentMinor)
            assertEquals(95_000L, stored.budgets.single().remainingMinor)
            assertEquals(2_000L, stored.transactions.first().updatedAt)
            assertEquals(budgetId, stored.transactions.first().budgetId)

            val reopened = RoomLedgerRepository(db, time)
            assertEquals(DataResult.Success(Unit), reopened.deleteBudget(budgetId))
            assertEquals(DataResult.Success(Unit), reopened.deleteCategory(categoryId))
            val remaining = db.moneyDao().transactions()
            assertEquals(2, remaining.size)
            remaining.forEach { assertNull(it.budgetId); assertNull(it.categoryId) }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
