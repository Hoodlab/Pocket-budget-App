package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class FakeTransactionRepository : TransactionRepository {
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    var shouldFail: Boolean = false
    var failureMessage: String = "Database error"

    override fun getTransactions(): Flow<List<Transaction>> = transactionsFlow.asStateFlow()

    override fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>> =
        transactionsFlow.map { list ->
            list.filter {
                val epoch = it.date.toEpochMilliseconds()
                epoch in startEpochMillis until endEpochMillis
            }.sortedByDescending { it.date.toEpochMilliseconds() }
        }

    override fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long> =
        transactionsFlow.map { list ->
            list.filter {
                it.type == org.hoods.pocket.domain.model.TransactionType.EXPENSE &&
                    it.date.toEpochMilliseconds() in startEpochMillis until endEpochMillis
            }.sumOf { it.amount }
        }

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
