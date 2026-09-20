package org.hoods.pocket.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncMutationDto(
    val mutationId: String,
    val deviceId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: JsonElement,
    val updatedAtEpochMillis: Long
)

@Serializable
data class SyncMutationResultDto(
    val mutationId: String,
    val status: String, // "APPLIED", "STALE", "REJECTED"
    val message: String? = null
)

@Serializable
data class PullSyncChangesResponseDto(
    val transactions: List<TransactionPullDto> = emptyList(),
    val monthlyBudgets: List<MonthlyBudgetPullDto> = emptyList(),
    val categoryBudgets: List<CategoryBudgetPullDto> = emptyList(),
    val serverEpochMillis: Long
)

@Serializable
data class TransactionPullDto(
    val id: String,
    val userId: String,
    val description: String,
    val amount: Long,
    val type: String,
    val category: String,
    val dateEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val deviceId: String = "",
    val mutationId: String = ""
)

@Serializable
data class MonthlyBudgetPullDto(
    val userId: String,
    val year: Int,
    val month: Int,
    val limitAmount: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val deviceId: String = "",
    val mutationId: String = ""
)

@Serializable
data class CategoryBudgetPullDto(
    val userId: String,
    val year: Int,
    val month: Int,
    val category: String,
    val limitAmount: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val deviceId: String = "",
    val mutationId: String = ""
)

interface RemoteSyncDataSource {
    suspend fun applySyncMutations(mutations: List<SyncMutationDto>): List<SyncMutationResultDto>
    suspend fun pullSyncChanges(sinceEpochMillis: Long): PullSyncChangesResponseDto
}
