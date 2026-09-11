package org.hoods.pocket.domain.repository

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.Transaction

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>>
    fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    suspend fun getTransactionById(id: String): Transaction?
}
