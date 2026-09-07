package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DeleteTransactionUseCaseTest {

    @Test
    fun `deleting a transaction removes it from repository`() = runTest {
        val repository = FakeTransactionRepository()
        val tx1 = Transaction(
            id = "tx-1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        val tx2 = Transaction(
            id = "tx-2",
            description = "Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171718000L)
        )
        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        val useCase = DeleteTransactionUseCase(repository)
        useCase("tx-1")

        val transactions = repository.getTransactions().first()
        assertEquals(listOf(tx2), transactions)
    }
}
