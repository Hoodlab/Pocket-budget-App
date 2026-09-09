package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.TransactionRepository

class GetTransactionsByMonthUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(month: YearMonth): Flow<List<Transaction>> {
        val startEpochMillis = month.startInstant().toEpochMilliseconds()
        val endEpochMillis = month.endInstant().toEpochMilliseconds()
        return repository.getTransactionsByDateRange(startEpochMillis, endEpochMillis)
    }
}
