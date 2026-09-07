package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class FakeTransactionRepository : TransactionRepository {
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    var shouldFail: Boolean = false
    var failureMessage: String = "Database error"

    override fun getTransactions(): Flow<List<Transaction>> = transactionsFlow.asStateFlow()

    override suspend fun insertTransaction(transaction: Transaction) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        transactionsFlow.value = transactionsFlow.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        transactionsFlow.value = transactionsFlow.value.map {
            if (it.id == transaction.id) transaction else it
        }
    }

    override suspend fun deleteTransaction(id: String) {
        if (shouldFail) throw IllegalStateException(failureMessage)
        transactionsFlow.value = transactionsFlow.value.filterNot { it.id == id }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        if (shouldFail) throw IllegalStateException(failureMessage)
        return transactionsFlow.value.find { it.id == id }
    }
}
