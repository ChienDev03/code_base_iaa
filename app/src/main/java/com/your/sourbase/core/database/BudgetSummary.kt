package com.your.sourbase.core.database

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

/** Computed expenses avoid stale totals and never combine different currencies. */
@DatabaseView(
    viewName = "budget_summary",
    value = """SELECT b.id, b.name, b.total_minor, b.currency, b.color,
        COALESCE(SUM(CASE WHEN t.transaction_type = 'expense' AND t.currency = b.currency
            THEN t.amount_minor ELSE 0 END), 0) AS spent_minor,
        b.total_minor - COALESCE(SUM(CASE WHEN t.transaction_type = 'expense' AND t.currency = b.currency
            THEN t.amount_minor ELSE 0 END), 0) AS remaining_minor
        FROM budgets b LEFT JOIN transactions t ON t.budget_id = b.id
        GROUP BY b.id, b.name, b.total_minor, b.currency, b.color""",
)
data class BudgetSummary(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    val currency: String,
    val color: Long?,
    @ColumnInfo(name = "spent_minor") val spentMinor: Long,
    @ColumnInfo(name = "remaining_minor") val remainingMinor: Long,
)
