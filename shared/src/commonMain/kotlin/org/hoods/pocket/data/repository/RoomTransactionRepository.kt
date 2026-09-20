package org.hoods.pocket.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.TransactionDao
import org.hoods.pocket.data.local.entity.TransactionEntity
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthRepository
import kotlin.time.Clock
import kotlin.time.Instant
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.data.sync.OutboxMutationBuilder
import org.hoods.pocket.data.sync.SyncJson

@OptIn(ExperimentalCoroutinesApi::class)
class RoomTransactionRepository(
    private val dao: TransactionDao,
    private val authRepository: AuthRepository? = null,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val deviceIdProvider: DeviceIdProvider? = null,
    private val activeUserId: () -> String? = { authRepository?.currentUser()?.id },
    private val activeUserIdFlow: Flow<String?>? = null
) : TransactionRepository {

    private val userFlow: Flow<String?>
        get() = (activeUserIdFlow
            ?: authRepository?.authState?.map { (it as? AuthState.Authenticated)?.user?.id }
            ?: flowOf(activeUserId())
        ).distinctUntilChanged()

    override fun getTransactions(): Flow<List<Transaction>> {
        return userFlow.flatMapLatest { userId ->
            dao.getAllTransactions(userId).map { entities ->
                entities.map { entity ->
                    Transaction(
                        id = entity.id,
                        description = entity.description,
                        amount = entity.amount,
                        type = TransactionType.valueOf(entity.type),
                        date = Instant.fromEpochMilliseconds(entity.dateEpochMillis),
                        category = TransactionCategory.fromName(entity.category)
                    )
                }
            }
        }
    }

    override fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>> {
        return userFlow.flatMapLatest { userId ->
            dao.getTransactionsByDateRange(startEpochMillis, endEpochMillis, userId).map { entities ->
                entities.map { entity ->
                    Transaction(
                        id = entity.id,
                        description = entity.description,
                        amount = entity.amount,
                        type = TransactionType.valueOf(entity.type),
                        date = Instant.fromEpochMilliseconds(entity.dateEpochMillis),
                        category = TransactionCategory.fromName(entity.category)
                    )
                }
            }
        }
    }

    override fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long> {
        return userFlow.flatMapLatest { userId ->
            dao.getExpenseTotalByDateRange(startEpochMillis, endEpochMillis, userId)
        }
    }

    override fun getIncomeTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long> {
        return userFlow.flatMapLatest { userId ->
            dao.getIncomeTotalByDateRange(startEpochMillis, endEpochMillis, userId)
        }
    }

    override fun getCategorySpendingByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Map<TransactionCategory, Long>> {
        return userFlow.flatMapLatest { userId ->
            dao.getCategorySpendingByDateRange(startEpochMillis, endEpochMillis, userId).map { list ->
                list.associate { entity ->
                    TransactionCategory.fromName(entity.category) to entity.totalSpent
                }
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = TransactionEntity(
            id = transaction.id,
            description = transaction.description,
            amount = transaction.amount,
            type = transaction.type.name,
            dateEpochMillis = transaction.date.toEpochMilliseconds(),
            category = transaction.category.name,
            userId = userId,
            updatedAtEpochMillis = now,
            deletedAtEpochMillis = null
        )
        val outbox = if (userId != null && deviceIdProvider != null) {
            OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "TRANSACTION",
                entityId = transaction.id,
                operation = "UPSERT",
                payloadJson = SyncJson.serializeTransaction(entity),
                now = now
            )
        } else null

        dao.insertWithOutbox(entity, outbox)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = TransactionEntity(
            id = transaction.id,
            description = transaction.description,
            amount = transaction.amount,
            type = transaction.type.name,
            dateEpochMillis = transaction.date.toEpochMilliseconds(),
            category = transaction.category.name,
            userId = userId,
            updatedAtEpochMillis = now,
            deletedAtEpochMillis = null
        )
        val outbox = if (userId != null && deviceIdProvider != null) {
            OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "TRANSACTION",
                entityId = transaction.id,
                operation = "UPSERT",
                payloadJson = SyncJson.serializeTransaction(entity),
                now = now
            )
        } else null

        dao.updateWithOutbox(entity, outbox)
    }

    override suspend fun deleteTransaction(id: String) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        if (userId != null && deviceIdProvider != null) {
            val existing = dao.getTransactionById(id, userId)
            val tombstone = existing?.copy(deletedAtEpochMillis = now, updatedAtEpochMillis = now)
                ?: TransactionEntity(
                    id = id,
                    description = "",
                    amount = 0L,
                    type = "EXPENSE",
                    dateEpochMillis = now,
                    userId = userId,
                    updatedAtEpochMillis = now,
                    deletedAtEpochMillis = now
                )
            val outbox = OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "TRANSACTION",
                entityId = id,
                operation = "DELETE",
                payloadJson = SyncJson.serializeTransaction(tombstone),
                now = now
            )
            dao.deleteWithOutbox(
                id = id,
                activeUserId = userId,
                isSoftDelete = true,
                deletedAt = now,
                updatedAt = now,
                outbox = outbox
            )
        } else {
            dao.deleteWithOutbox(
                id = id,
                activeUserId = userId,
                isSoftDelete = false,
                deletedAt = now,
                updatedAt = now,
                outbox = null
            )
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = dao.getTransactionById(id, activeUserId()) ?: return null
        return Transaction(
            id = entity.id,
            description = entity.description,
            amount = entity.amount,
            type = TransactionType.valueOf(entity.type),
            date = Instant.fromEpochMilliseconds(entity.dateEpochMillis),
            category = TransactionCategory.fromName(entity.category)
        )
    }
}
