package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.data.local.entity.TransactionEntity

data class CategorySpendingEntity(
    val category: String,
    val totalSpent: Long
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId) ORDER BY dateEpochMillis DESC")
    fun getAllTransactions(activeUserId: String? = null): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) ORDER BY dateEpochMillis DESC")
    fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    fun getIncomeTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(amount), 0) AS totalSpent FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId)) GROUP BY category")
    fun getCategorySpendingByDateRange(startEpochMillis: Long, endEpochMillis: Long, activeUserId: String? = null): Flow<List<CategorySpendingEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun getTransactionById(id: String, activeUserId: String? = null): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id AND ((:activeUserId IS NULL AND userId IS NULL) OR (userId = :activeUserId))")
    suspend fun deleteTransactionById(id: String, activeUserId: String? = null)
}
