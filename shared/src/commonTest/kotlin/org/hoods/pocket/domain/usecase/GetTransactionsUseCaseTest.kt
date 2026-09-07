package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetTransactionsUseCaseTest {

    @Test
    fun `returns transactions flow from repository`() = runTest {
        val repository = FakeTransactionRepository()
        val tx = Transaction(
            id = "tx-1",
            description = "Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(tx)

        val useCase = GetTransactionsUseCase(repository)
        val result = useCase().first()

        assertEquals(listOf(tx), result)
    }
}
