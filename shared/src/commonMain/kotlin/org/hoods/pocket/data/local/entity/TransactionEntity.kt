package org.hoods.pocket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val description: String,
    val amount: Long,
    val type: String,
    val dateEpochMillis: Long,
    val category: String = "UNCATEGORIZED"
)
