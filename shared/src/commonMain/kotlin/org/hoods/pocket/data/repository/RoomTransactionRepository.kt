package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.TransactionDao
import org.hoods.pocket.data.local.entity.TransactionEntity
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.repository.TransactionRepository
import kotlin.time.Instant

class RoomTransactionRepository(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { entity ->
                Transaction(
                    id = entity.id,
                    description = entity.description,
                    amount = entity.amount,
                    type = TransactionType.valueOf(entity.type),
                    date = Instant.fromEpochMilliseconds(entity.dateEpochMillis)
                )
            }
        }
    }

    override fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>> {
        return dao.getTransactionsByDateRange(startEpochMillis, endEpochMillis).map { entities ->
            entities.map { entity ->
                Transaction(
                    id = entity.id,
                    description = entity.description,
                    amount = entity.amount,
                    type = TransactionType.valueOf(entity.type),
                    date = Instant.fromEpochMilliseconds(entity.dateEpochMillis)
                )
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(
            TransactionEntity(
                id = transaction.id,
                description = transaction.description,
                amount = transaction.amount,
                type = transaction.type.name,
                dateEpochMillis = transaction.date.toEpochMilliseconds()
            )
        )
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.updateTransaction(
            TransactionEntity(
                id = transaction.id,
                description = transaction.description,
                amount = transaction.amount,
                type = transaction.type.name,
                dateEpochMillis = transaction.date.toEpochMilliseconds()
            )
        )
    }

    override suspend fun deleteTransaction(id: String) {
        dao.deleteTransactionById(id)
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = dao.getTransactionById(id) ?: return null
        return Transaction(
            id = entity.id,
            description = entity.description,
            amount = entity.amount,
            type = TransactionType.valueOf(entity.type),
            date = Instant.fromEpochMilliseconds(entity.dateEpochMillis)
        )
    }
}
