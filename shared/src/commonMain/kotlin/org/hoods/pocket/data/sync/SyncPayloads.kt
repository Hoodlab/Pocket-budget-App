package org.hoods.pocket.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity

@Serializable
data class TransactionSyncPayload(
    val id: String,
    val userId: String,
    val description: String,
    val amount: Long,
    val type: String,
    val category: String,
    val dateEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)

@Serializable
data class MonthlyBudgetSyncPayload(
    val userId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)

@Serializable
data class CategoryBudgetSyncPayload(
    val userId: String,
    val year: Int,
    val month: Int,
    val category: String,
    val limitAmount: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)

object SyncJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serializeTransaction(entity: TransactionEntity): String {
        val payload = TransactionSyncPayload(
            id = entity.id,
            userId = entity.userId ?: "",
            description = entity.description,
            amount = entity.amount,
            type = entity.type,
            category = entity.category,
            dateEpochMillis = entity.dateEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            deletedAtEpochMillis = entity.deletedAtEpochMillis
        )
        return json.encodeToString(payload)
    }

    fun deserializeTransaction(jsonString: String): TransactionSyncPayload {
        return json.decodeFromString(jsonString)
    }

    fun serializeMonthlyBudget(entity: MonthlyBudgetEntity): String {
        val payload = MonthlyBudgetSyncPayload(
            userId = entity.userId ?: "",
            year = entity.year,
            month = entity.month,
            limitAmount = entity.limitAmount,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            deletedAtEpochMillis = entity.deletedAtEpochMillis
        )
        return json.encodeToString(payload)
    }

    fun deserializeMonthlyBudget(jsonString: String): MonthlyBudgetSyncPayload {
        return json.decodeFromString(jsonString)
    }

    fun serializeCategoryBudget(entity: CategoryBudgetEntity): String {
        val payload = CategoryBudgetSyncPayload(
            userId = entity.userId ?: "",
            year = entity.year,
            month = entity.month,
            category = entity.category,
            limitAmount = entity.limitAmount,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            deletedAtEpochMillis = entity.deletedAtEpochMillis
        )
        return json.encodeToString(payload)
    }

    fun deserializeCategoryBudget(jsonString: String): CategoryBudgetSyncPayload {
        return json.decodeFromString(jsonString)
    }
}
