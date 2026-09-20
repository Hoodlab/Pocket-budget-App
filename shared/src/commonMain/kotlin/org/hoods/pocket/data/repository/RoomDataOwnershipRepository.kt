package org.hoods.pocket.data.repository

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.hoods.pocket.data.local.dao.DataOwnershipDao
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.data.sync.SyncJson
import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.DataOwnershipRepository

@OptIn(ExperimentalUuidApi::class)
class RoomDataOwnershipRepository(
    private val dao: DataOwnershipDao,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val deviceIdProvider: DeviceIdProvider? = null
) : DataOwnershipRepository {

    override suspend fun getUnclaimedDataSummary(): UnclaimedDataSummary {
        val txCount = dao.getUnclaimedTransactionCount()
        val budgetCount = dao.getUnclaimedMonthlyBudgetCount()
        val catCount = dao.getUnclaimedCategoryBudgetCount()
        return UnclaimedDataSummary(
            transactionCount = txCount,
            monthlyBudgetCount = budgetCount,
            categoryBudgetCount = catCount
        )
    }

    override suspend fun claimLocalDataAtomically(userId: String): Result<Unit> {
        return runCatching {
            val now = Clock.System.now().toEpochMilliseconds()
            if (syncOutboxDao != null && deviceIdProvider != null) {
                val unclaimedTx = dao.getUnclaimedTransactions()
                val unclaimedBudgets = dao.getUnclaimedMonthlyBudgets()
                val unclaimedCats = dao.getUnclaimedCategoryBudgets()

                val mutations = mutableListOf<SyncOutboxEntity>()

                for (tx in unclaimedTx) {
                    val updated = tx.copy(userId = userId, updatedAtEpochMillis = now)
                    mutations.add(
                        SyncOutboxEntity(
                            mutationId = Uuid.random().toString(),
                            userId = userId,
                            deviceId = deviceIdProvider.getDeviceId(),
                            entityType = "TRANSACTION",
                            entityId = tx.id,
                            operation = "UPSERT",
                            payloadJson = SyncJson.serializeTransaction(updated),
                            updatedAtEpochMillis = now,
                            status = "PENDING",
                            retryCount = 0,
                            lastError = null,
                            createdAtEpochMillis = now
                        )
                    )
                }

                for (budget in unclaimedBudgets) {
                    val updated = budget.copy(userId = userId, updatedAtEpochMillis = now)
                    mutations.add(
                        SyncOutboxEntity(
                            mutationId = Uuid.random().toString(),
                            userId = userId,
                            deviceId = deviceIdProvider.getDeviceId(),
                            entityType = "MONTHLY_BUDGET",
                            entityId = "${budget.year}-${budget.month}",
                            operation = "UPSERT",
                            payloadJson = SyncJson.serializeMonthlyBudget(updated),
                            updatedAtEpochMillis = now,
                            status = "PENDING",
                            retryCount = 0,
                            lastError = null,
                            createdAtEpochMillis = now
                        )
                    )
                }

                for (cat in unclaimedCats) {
                    val updated = cat.copy(userId = userId, updatedAtEpochMillis = now)
                    mutations.add(
                        SyncOutboxEntity(
                            mutationId = Uuid.random().toString(),
                            userId = userId,
                            deviceId = deviceIdProvider.getDeviceId(),
                            entityType = "CATEGORY_BUDGET",
                            entityId = "${cat.year}-${cat.month}-${cat.category}",
                            operation = "UPSERT",
                            payloadJson = SyncJson.serializeCategoryBudget(updated),
                            updatedAtEpochMillis = now,
                            status = "PENDING",
                            retryCount = 0,
                            lastError = null,
                            createdAtEpochMillis = now
                        )
                    )
                }

                dao.claimAllUnassignedWithMutations(userId, now, mutations)
            } else {
                dao.claimAllUnassigned(userId)
            }
        }
    }
}
