package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class AddTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.insertTransaction(transaction)
    }
}
