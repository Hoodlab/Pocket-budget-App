package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetCashFlowSummaryUseCaseTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var useCase: GetCashFlowSummaryUseCase

    private val testMonth = YearMonth(2026, 8)

    @BeforeTest
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        useCase = GetCashFlowSummaryUseCase(transactionRepository)
    }

    @Test
    fun calculatesMonthlyIncomeExpenseAndNetBalance() = runTest {
        // Add income: $4,500.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-inc",
                description = "Salary",
                amount = 450000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-08-01T10:00:00Z"),
                category = TransactionCategory.SALARY
            )
        )

        // Add expense: $2,580.00
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-exp",
                description = "Groceries and dining",
                amount = 258000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-15T12:00:00Z"),
                category = TransactionCategory.GROCERIES
            )
        )

        val summary = useCase(testMonth).first()

        assertEquals(testMonth, summary.yearMonth)
        assertEquals(450000L, summary.income)
        assertEquals(258000L, summary.expense)
        assertEquals(192000L, summary.netBalance)
    }

    @Test
    fun negativeNetBalance_whenExpensesExceedIncome() = runTest {
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-inc",
                description = "Gift",
                amount = 100000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-08-05T10:00:00Z"),
                category = TransactionCategory.GIFT
            )
        )

        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-exp",
                description = "Rent",
                amount = 150000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-08-01T10:00:00Z"),
                category = TransactionCategory.BILLS
            )
        )

        val summary = useCase(testMonth).first()

        assertEquals(100000L, summary.income)
        assertEquals(150000L, summary.expense)
        assertEquals(-50000L, summary.netBalance)
    }

    @Test
    fun emptyMonth_returnsZeroIncomeExpenseAndNetBalance() = runTest {
        val summary = useCase(testMonth).first()

        assertEquals(0L, summary.income)
        assertEquals(0L, summary.expense)
        assertEquals(0L, summary.netBalance)
    }

    @Test
    fun monthIsolation_transactionsInOtherMonths_ignored() = runTest {
        val prevMonth = YearMonth(2026, 7)
        val nextMonth = YearMonth(2026, 9)

        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-prev-inc",
                description = "July Salary",
                amount = 500000L,
                type = TransactionType.INCOME,
                date = Instant.parse("2026-07-25T10:00:00Z"),
                category = TransactionCategory.SALARY
            )
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-next-exp",
                description = "Sept Rent",
                amount = 200000L,
                type = TransactionType.EXPENSE,
                date = Instant.parse("2026-09-02T10:00:00Z"),
                category = TransactionCategory.BILLS
            )
        )

        val summary = useCase(testMonth).first()

        assertEquals(0L, summary.income)
        assertEquals(0L, summary.expense)
        assertEquals(0L, summary.netBalance)
    }
}
