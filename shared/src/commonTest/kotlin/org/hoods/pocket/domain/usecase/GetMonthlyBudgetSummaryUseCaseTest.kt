package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GetMonthlyBudgetSummaryUseCaseTest {

    @Test
    fun noBudgetSet_returnsNullBudgetAndZeroProgress() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val targetMonth = YearMonth(2026, 9)

        val summary = useCase(targetMonth).first()

        assertEquals(targetMonth, summary.yearMonth)
        assertNull(summary.budget)
        assertEquals(0L, summary.totalExpense)
        assertNull(summary.remainingAmount)
        assertEquals(0f, summary.progress)
        assertNull(summary.status)
    }

    @Test
    fun budgetSet_calculatesExpenseOnlySpending_ignoringIncomeAndOtherMonths() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val targetMonth = YearMonth(2026, 9)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 10000L)) // $100.00

        // In-month expenses
        transactionRepository.insertTransaction(
            Transaction("1", "Groceries", 4550L, TransactionType.EXPENSE, Instant.parse("2026-09-05T12:00:00Z"))
        )
        transactionRepository.insertTransaction(
            Transaction("2", "Coffee", 2000L, TransactionType.EXPENSE, Instant.parse("2026-09-10T12:00:00Z"))
        )
        // In-month income (MUST BE IGNORED)
        transactionRepository.insertTransaction(
            Transaction("3", "Salary", 300000L, TransactionType.INCOME, Instant.parse("2026-09-01T12:00:00Z"))
        )
        // Previous month expense (MUST BE IGNORED)
        transactionRepository.insertTransaction(
            Transaction("4", "August Dinner", 5000L, TransactionType.EXPENSE, Instant.parse("2026-08-25T12:00:00Z"))
        )

        val summary = useCase(targetMonth).first()

        assertEquals(targetMonth, summary.yearMonth)
        assertEquals(10000L, summary.budget?.limitAmount)
        assertEquals(6550L, summary.totalExpense)
        assertEquals(3450L, summary.remainingAmount)
        assertEquals(0.655f, summary.progress, 0.001f)
        assertEquals(BudgetHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun nearLimit_whenProgressBetween80And100Percent() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val targetMonth = YearMonth(2026, 9)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 10000L)) // $100.00
        transactionRepository.insertTransaction(
            Transaction("1", "Shopping", 8500L, TransactionType.EXPENSE, Instant.parse("2026-09-15T12:00:00Z"))
        )

        val summary = useCase(targetMonth).first()

        assertEquals(8500L, summary.totalExpense)
        assertEquals(1500L, summary.remainingAmount)
        assertEquals(0.85f, summary.progress, 0.001f)
        assertEquals(BudgetHealthStatus.NEAR_LIMIT, summary.status)
    }

    @Test
    fun overBudget_whenProgressExceeds100Percent() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val transactionRepository = FakeTransactionRepository()
        val useCase = GetMonthlyBudgetSummaryUseCase(budgetRepository, transactionRepository)
        val targetMonth = YearMonth(2026, 9)

        budgetRepository.setMonthlyBudget(MonthlyBudget(targetMonth, 10000L)) // $100.00
        transactionRepository.insertTransaction(
            Transaction("1", "Shopping", 12500L, TransactionType.EXPENSE, Instant.parse("2026-09-15T12:00:00Z"))
        )

        val summary = useCase(targetMonth).first()

        assertEquals(12500L, summary.totalExpense)
        assertEquals(-2500L, summary.remainingAmount)
        assertEquals(1.25f, summary.progress, 0.001f)
        assertEquals(BudgetHealthStatus.OVER_BUDGET, summary.status)
    }
}
