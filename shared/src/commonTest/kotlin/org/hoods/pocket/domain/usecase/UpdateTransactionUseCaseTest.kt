package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class UpdateTransactionUseCaseTest {

    @Test
    fun `updating a transaction updates existing record without creating duplicate`() = runTest {
        val repository = FakeTransactionRepository()
        val original = Transaction(
            id = "tx-1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val useCase = UpdateTransactionUseCase(repository)
        val updated = original.copy(
            description = "Supermarket",
            amount = 5200L,
            type = TransactionType.EXPENSE
        )

        useCase(updated)

        val transactions = repository.getTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(updated, transactions.first())
    }
}
