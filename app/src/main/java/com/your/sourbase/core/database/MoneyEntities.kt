package com.your.sourbase.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Supported local ledger entry types. */
enum class TransactionType(val storedValue: String) {
    Income("income"), Expense("expense"), Loan("loan")
}

/** Stable SQL values independent of Kotlin enum names. */
class MoneyConverters {
    /** Encodes the schema's lowercase ledger type. */
    @TypeConverter fun encode(value: TransactionType): String = value.storedValue
    /** Rejects unknown stored types instead of silently reclassifying a transaction. */
    @TypeConverter fun decode(value: String): TransactionType =
        TransactionType.entries.first { it.storedValue == value }
}

/** Local category; timestamps use Unix seconds to match the reference schema. */
@Entity(tableName = "categories", indices = [Index(value = ["name", "transaction_type"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
    @ColumnInfo(name = "icon_resource") val iconResource: Int? = null,
    val color: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Budget amount is an integer in the currency's minor unit, never floating point. */
@Entity(tableName = "budgets", indices = [Index(value = ["name"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    val currency: String,
    val color: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Durable ledger row; deleting a category or budget preserves its transactions. */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = BudgetEntity::class, parentColumns = ["id"], childColumns = ["budget_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["occurred_at"]), Index(value = ["transaction_type", "occurred_at"]),
        Index(value = ["budget_id", "occurred_at"]), Index(value = ["category_id", "occurred_at"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
    val currency: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "budget_id") val budgetId: Long? = null,
    val note: String = "",
    val lender: String = "",
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
