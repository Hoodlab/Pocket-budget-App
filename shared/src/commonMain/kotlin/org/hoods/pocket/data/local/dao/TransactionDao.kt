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
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis ORDER BY dateEpochMillis DESC")
    fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis")
    fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis")
    fun getIncomeTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(amount), 0) AS totalSpent FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis GROUP BY category")
    fun getCategorySpendingByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<CategorySpendingEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)
}
