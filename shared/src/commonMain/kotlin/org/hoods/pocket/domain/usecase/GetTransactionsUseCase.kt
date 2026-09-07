package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class GetTransactionsUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getTransactions()
}
