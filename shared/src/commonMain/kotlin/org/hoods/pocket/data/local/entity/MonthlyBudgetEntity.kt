package org.hoods.pocket.data.local.entity

import androidx.room.Entity

@Entity(tableName = "monthly_budgets", primaryKeys = ["year", "month"])
data class MonthlyBudgetEntity(
    val year: Int,
    val month: Int,
    val limitAmount: Long,
    val userId: String? = null
)
