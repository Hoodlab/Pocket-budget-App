package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class GetTransactionByIdUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: String): Transaction? = repository.getTransactionById(id)
}
