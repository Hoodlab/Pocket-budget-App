package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.data.local.entity.TransactionEntity

data class CategorySpendingEntity(
    val category: String,
    val totalSpent: Long
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL) ORDER BY dateEpochMillis DESC")
    fun getAllTransactions(activeUserId: String? = null): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL) ORDER BY dateEpochMillis DESC")
    fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    fun getIncomeTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(amount), 0) AS totalSpent FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL) GROUP BY category")
    fun getCategorySpendingByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<List<CategorySpendingEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND (((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) AND deletedAtEpochMillis IS NULL)")
    suspend fun getTransactionById(id: String, activeUserId: String? = null): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun getTransactionByIdIncludingDeleted(id: String, activeUserId: String? = null): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteTransactionById(id: String, activeUserId: String? = null)

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAtEpochMillis, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun softDeleteTransactionById(id: String, deletedAtEpochMillis: Long, updatedAtEpochMillis: Long, activeUserId: String? = null)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOutbox(mutation: SyncOutboxEntity)

    @Transaction
    suspend fun insertWithOutbox(transaction: TransactionEntity, outbox: SyncOutboxEntity?) {
        insertTransaction(transaction)
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun updateWithOutbox(transaction: TransactionEntity, outbox: SyncOutboxEntity?) {
        updateTransaction(transaction)
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }

    @Transaction
    suspend fun deleteWithOutbox(
        id: String,
        activeUserId: String?,
        isSoftDelete: Boolean,
        deletedAt: Long,
        updatedAt: Long,
        outbox: SyncOutboxEntity?
    ) {
        if (isSoftDelete) {
            softDeleteTransactionById(id, deletedAt, updatedAt, activeUserId)
        } else {
            deleteTransactionById(id, activeUserId)
        }
        if (outbox != null) {
            insertOrUpdateOutbox(outbox)
        }
    }
}
