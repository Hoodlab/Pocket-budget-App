package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetTransactionsByMonthUseCaseTest {

    @Test
    fun returnsOnlyTransactionsForSpecifiedMonthOrderedDescending() = runTest {
        val repository = FakeTransactionRepository()
        val mayMonth = YearMonth(2026, 5)

        val aprilTx = Transaction(
            id = "tx-apr",
            description = "April Dinner",
            amount = 4500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-04-30T23:59:59Z")
        )
        val mayEarlyTx = Transaction(
            id = "tx-may-1",
            description = "May Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.parse("2026-05-01T00:00:00Z")
        )
        val mayLateTx = Transaction(
            id = "tx-may-2",
            description = "May Groceries",
            amount = 5200L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-05-20T15:30:00Z")
        )
        val juneTx = Transaction(
            id = "tx-jun",
            description = "June Rent",
            amount = 120000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-06-01T00:00:00Z")
        )

        repository.insertTransaction(aprilTx)
        repository.insertTransaction(mayEarlyTx)
        repository.insertTransaction(mayLateTx)
        repository.insertTransaction(juneTx)

        val useCase = GetTransactionsByMonthUseCase(repository)
        val result = useCase(mayMonth).first()

        assertEquals(listOf(mayLateTx, mayEarlyTx), result)
    }
}
