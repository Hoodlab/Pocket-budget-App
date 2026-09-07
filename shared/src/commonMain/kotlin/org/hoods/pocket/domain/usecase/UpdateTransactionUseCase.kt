package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.updateTransaction(transaction)
    }
}
