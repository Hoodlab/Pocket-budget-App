package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.hoods.pocket.domain.model.CashFlowSummary
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.TransactionRepository

class GetCashFlowSummaryUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<CashFlowSummary> {
        val startMillis = yearMonth.startInstant().toEpochMilliseconds()
        val endMillis = yearMonth.endInstant().toEpochMilliseconds()

        val incomeFlow = transactionRepository.getIncomeTotalByDateRange(startMillis, endMillis)
        val expenseFlow = transactionRepository.getExpenseTotalByDateRange(startMillis, endMillis)

        return combine(incomeFlow, expenseFlow) { income, expense ->
            CashFlowSummary(
                yearMonth = yearMonth,
                income = income,
                expense = expense,
                netBalance = income - expense
            )
        }
    }
}
