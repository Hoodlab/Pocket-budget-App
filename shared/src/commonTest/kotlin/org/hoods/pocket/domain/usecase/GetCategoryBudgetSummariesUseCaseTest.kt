package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class GetCategoryBudgetSummariesUseCaseTest {

    private lateinit var categoryBudgetRepository: FakeCategoryBudgetRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var useCase: GetCategoryBudgetSummariesUseCase

    private val testMonth = YearMonth(2026, 3)

    @BeforeTest
    fun setUp() {
        categoryBudgetRepository = FakeCategoryBudgetRepository()
        transactionRepository = FakeTransactionRepository()
        useCase = GetCategoryBudgetSummariesUseCase(
            categoryBudgetRepository = categoryBudgetRepository,
            transactionRepository = transactionRepository
        )
    }

    @Test
    fun categorisedExpenses_mappedToCategorySummaries_withCorrectSpentRemainingProgressAndHealth() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 60000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-1",
                description = "Weekly shopping",
                amount = 45000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.GROCERIES
            )
        )

        val summaries = useCase(testMonth).first()

        assertEquals(1, summaries.size)
        val summary = summaries.first()
        assertEquals(TransactionCategory.GROCERIES, summary.category)
        assertEquals(60000L, summary.limitAmount)
        assertEquals(45000L, summary.spentAmount)
        assertEquals(15000L, summary.remainingAmount)
        assertEquals(0.75f, summary.progress)
        assertEquals(BudgetHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun incomeTransactions_doNotAffectCategoryHealth() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 50000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-income",
                description = "Bonus or refund",
                amount = 20000L,
                type = TransactionType.INCOME,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.SALARY
            )
        )

        val summaries = useCase(testMonth).first()

        assertEquals(1, summaries.size)
        val summary = summaries.first()
        assertEquals(0L, summary.spentAmount)
        assertEquals(50000L, summary.remainingAmount)
        assertEquals(0f, summary.progress)
        assertEquals(BudgetHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun uncategorizedExpense_doesNotProduceCategorySummary() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 50000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-uncategorized",
                description = "Miscellaneous",
                amount = 15000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.UNCATEGORIZED
            )
        )

        val summaries = useCase(testMonth).first()

        assertEquals(1, summaries.size)
        assertEquals(TransactionCategory.GROCERIES, summaries.first().category)
        assertEquals(0L, summaries.first().spentAmount)
    }

    @Test
    fun unallocatedExpenseCategory_doesNotProduceCategorySummary() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 50000L)
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-dining",
                description = "Restaurant",
                amount = 25000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.DINING
            )
        )

        val summaries = useCase(testMonth).first()

        assertEquals(1, summaries.size)
        assertEquals(TransactionCategory.GROCERIES, summaries.first().category)
        assertTrue(summaries.none { it.category == TransactionCategory.DINING })
    }

    @Test
    fun nearLimitAndOverBudgetThresholds_correctlyEvaluated() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 10000L)
        )
        // 80% threshold is 8000L -> NEAR_LIMIT
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-near",
                description = "80 percent spent",
                amount = 8000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.GROCERIES
            )
        )

        var summaries = useCase(testMonth).first()
        assertEquals(BudgetHealthStatus.NEAR_LIMIT, summaries.first().status)
        assertEquals(0.8f, summaries.first().progress)

        // 10001L -> OVER_BUDGET
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-over",
                description = "Over budget",
                amount = 2001L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(testMonth.startInstant().toEpochMilliseconds() + 2000L),
                category = TransactionCategory.GROCERIES
            )
        )

        summaries = useCase(testMonth).first()
        assertEquals(10001L, summaries.first().spentAmount)
        assertEquals(-1L, summaries.first().remainingAmount)
        assertEquals(BudgetHealthStatus.OVER_BUDGET, summaries.first().status)
    }

    @Test
    fun monthIsolation_transactionsInOtherMonths_ignored() = runTest {
        categoryBudgetRepository.setCategoryBudget(
            CategoryBudget(yearMonth = testMonth, category = TransactionCategory.GROCERIES, limitAmount = 50000L)
        )
        val prevMonth = YearMonth(2026, 2)
        val nextMonth = YearMonth(2026, 4)

        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-prev",
                description = "Previous month expense",
                amount = 20000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(prevMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.GROCERIES
            )
        )
        transactionRepository.insertTransaction(
            Transaction(
                id = "tx-next",
                description = "Next month expense",
                amount = 30000L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(nextMonth.startInstant().toEpochMilliseconds() + 1000L),
                category = TransactionCategory.GROCERIES
            )
        )

        val summaries = useCase(testMonth).first()
        assertEquals(1, summaries.size)
        assertEquals(0L, summaries.first().spentAmount)
    }
}
