package org.hoods.pocket.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.hoods.pocket.data.local.dao.BudgetDao
import org.hoods.pocket.data.local.dao.CategoryBudgetDao
import org.hoods.pocket.data.local.dao.SyncMetadataDao
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.local.dao.TransactionDao
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.SyncMetadataEntity
import org.hoods.pocket.data.local.entity.TransactionEntity
import kotlin.time.Clock

sealed interface SyncResult {
    data class Success(val pushedCount: Int, val pulledCount: Int) : SyncResult
    data class Failure(val message: String, val canRetry: Boolean) : SyncResult
    data object Unavailable : SyncResult
}

interface SyncEngine {
    suspend fun synchronize(userId: String): SyncResult
}

class PocketSyncEngine(
    private val syncOutboxDao: SyncOutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val remoteDataSource: RemoteSyncDataSource?,
    private val json: Json = SyncJson.json
) : SyncEngine {

    override suspend fun synchronize(userId: String): SyncResult {
        if (remoteDataSource == null) {
            return SyncResult.Unavailable
        }

        return try {
            var pushedCount = 0
            var pulledCount = 0

            // 1. Push pending mutations
            val pending = syncOutboxDao.getPendingMutations(userId)
            if (pending.isNotEmpty()) {
                val mutationDtos = pending.map { entity ->
                    val parsedPayload: JsonElement = json.parseToJsonElement(entity.payloadJson)
                    SyncMutationDto(
                        mutationId = entity.mutationId,
                        deviceId = entity.deviceId,
                        entityType = entity.entityType,
                        entityId = entity.entityId,
                        operation = entity.operation,
                        payload = parsedPayload,
                        updatedAtEpochMillis = entity.updatedAtEpochMillis
                    )
                }

                val results = remoteDataSource.applySyncMutations(mutationDtos)
                for (result in results) {
                    when (result.status) {
                        "APPLIED", "STALE" -> {
                            syncOutboxDao.deleteMutation(result.mutationId)
                            pushedCount++
                        }
                        "REJECTED" -> {
                            val original = pending.firstOrNull { it.mutationId == result.mutationId }
                            if (original != null) {
                                syncOutboxDao.updateMutation(
                                    original.copy(
                                        status = "FAILED",
                                        retryCount = original.retryCount + 1,
                                        lastError = result.message ?: "Rejected by server"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Pull remote changes
            val metadata = syncMetadataDao.getMetadata(userId)
            val currentCursor = metadata?.lastSyncCursor?.toLongOrNull() ?: 0L
            val pullResponse = remoteDataSource.pullSyncChanges(currentCursor)

            // Reconcile Transactions
            for (remoteTx in pullResponse.transactions) {
                val localPending = syncOutboxDao.findPendingMutation(userId, "TRANSACTION", remoteTx.id)
                if (localPending != null) {
                    val incomingWins = SyncConflictResolver.incomingWins(
                        incomingUpdatedAt = remoteTx.updatedAtEpochMillis,
                        incomingDeviceId = remoteTx.deviceId,
                        incomingMutationId = remoteTx.mutationId,
                        existingUpdatedAt = localPending.updatedAtEpochMillis,
                        existingDeviceId = localPending.deviceId,
                        existingMutationId = localPending.mutationId
                    )
                    if (incomingWins) {
                        applyRemoteTransaction(remoteTx, userId)
                        syncOutboxDao.deleteMutation(localPending.mutationId)
                        pulledCount++
                    }
                } else {
                    applyRemoteTransaction(remoteTx, userId)
                    pulledCount++
                }
            }

            // Reconcile Monthly Budgets
            for (remoteMb in pullResponse.monthlyBudgets) {
                val entityId = "${remoteMb.year}-${remoteMb.month}"
                val localPending = syncOutboxDao.findPendingMutation(userId, "MONTHLY_BUDGET", entityId)
                if (localPending != null) {
                    val incomingWins = SyncConflictResolver.incomingWins(
                        incomingUpdatedAt = remoteMb.updatedAtEpochMillis,
                        incomingDeviceId = remoteMb.deviceId,
                        incomingMutationId = remoteMb.mutationId,
                        existingUpdatedAt = localPending.updatedAtEpochMillis,
                        existingDeviceId = localPending.deviceId,
                        existingMutationId = localPending.mutationId
                    )
                    if (incomingWins) {
                        applyRemoteMonthlyBudget(remoteMb, userId)
                        syncOutboxDao.deleteMutation(localPending.mutationId)
                        pulledCount++
                    }
                } else {
                    applyRemoteMonthlyBudget(remoteMb, userId)
                    pulledCount++
                }
            }

            // Reconcile Category Budgets
            for (remoteCb in pullResponse.categoryBudgets) {
                val entityId = "${remoteCb.year}-${remoteCb.month}-${remoteCb.category}"
                val localPending = syncOutboxDao.findPendingMutation(userId, "CATEGORY_BUDGET", entityId)
                if (localPending != null) {
                    val incomingWins = SyncConflictResolver.incomingWins(
                        incomingUpdatedAt = remoteCb.updatedAtEpochMillis,
                        incomingDeviceId = remoteCb.deviceId,
                        incomingMutationId = remoteCb.mutationId,
                        existingUpdatedAt = localPending.updatedAtEpochMillis,
                        existingDeviceId = localPending.deviceId,
                        existingMutationId = localPending.mutationId
                    )
                    if (incomingWins) {
                        applyRemoteCategoryBudget(remoteCb, userId)
                        syncOutboxDao.deleteMutation(localPending.mutationId)
                        pulledCount++
                    }
                } else {
                    applyRemoteCategoryBudget(remoteCb, userId)
                    pulledCount++
                }
            }

            // 3. Advance sync cursor durably
            val now = Clock.System.now().toEpochMilliseconds()
            syncMetadataDao.upsertMetadata(
                SyncMetadataEntity(
                    userId = userId,
                    lastSyncCursor = pullResponse.serverEpochMillis.toString(),
                    lastSyncAtEpochMillis = now,
                    lastSyncStatus = "IDLE",
                    lastSyncError = null
                )
            )

            SyncResult.Success(pushedCount = pushedCount, pulledCount = pulledCount)
        } catch (e: Throwable) {
            val now = Clock.System.now().toEpochMilliseconds()
            val existing = syncMetadataDao.getMetadata(userId)
            syncMetadataDao.upsertMetadata(
                SyncMetadataEntity(
                    userId = userId,
                    lastSyncCursor = existing?.lastSyncCursor,
                    lastSyncAtEpochMillis = existing?.lastSyncAtEpochMillis ?: now,
                    lastSyncStatus = "ERROR",
                    lastSyncError = e.message ?: "Unknown sync error"
                )
            )
            SyncResult.Failure(message = e.message ?: "Sync failed", canRetry = true)
        }
    }

    private suspend fun applyRemoteTransaction(tx: TransactionPullDto, userId: String) {
        val entity = TransactionEntity(
            id = tx.id,
            description = tx.description,
            amount = tx.amount,
            type = tx.type,
            category = tx.category,
            dateEpochMillis = tx.dateEpochMillis,
            userId = userId,
            updatedAtEpochMillis = tx.updatedAtEpochMillis,
            deletedAtEpochMillis = tx.deletedAtEpochMillis
        )
        transactionDao.insertTransaction(entity)
    }

    private suspend fun applyRemoteMonthlyBudget(mb: MonthlyBudgetPullDto, userId: String) {
        val entity = MonthlyBudgetEntity(
            year = mb.year,
            month = mb.month,
            limitAmount = mb.limitAmount,
            userId = userId,
            updatedAtEpochMillis = mb.updatedAtEpochMillis,
            deletedAtEpochMillis = mb.deletedAtEpochMillis
        )
        budgetDao.insertOrUpdateMonthlyBudget(entity)
    }

    private suspend fun applyRemoteCategoryBudget(cb: CategoryBudgetPullDto, userId: String) {
        val entity = CategoryBudgetEntity(
            year = cb.year,
            month = cb.month,
            category = cb.category,
            limitAmount = cb.limitAmount,
            userId = userId,
            updatedAtEpochMillis = cb.updatedAtEpochMillis,
            deletedAtEpochMillis = cb.deletedAtEpochMillis
        )
        categoryBudgetDao.insertOrUpdateCategoryBudget(entity)
    }
}
