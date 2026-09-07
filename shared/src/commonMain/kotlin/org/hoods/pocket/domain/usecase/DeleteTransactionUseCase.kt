package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteTransaction(id)
    }
}
