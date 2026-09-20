package org.hoods.pocket.data.local.entity

import androidx.room.Entity

@Entity(tableName = "category_budgets", primaryKeys = ["year", "month", "category"])
data class CategoryBudgetEntity(
    val year: Int,
    val month: Int,
    val category: String,
    val limitAmount: Long,
    val userId: String? = null
)
