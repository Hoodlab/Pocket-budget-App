package org.hoods.pocket.domain.repository

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.Transaction

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
}
