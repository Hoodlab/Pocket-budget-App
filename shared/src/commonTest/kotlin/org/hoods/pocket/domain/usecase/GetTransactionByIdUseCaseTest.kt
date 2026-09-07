package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class GetTransactionByIdUseCaseTest {

    @Test
    fun `returns transaction when transaction exists in repository`() = runTest {
        val repository = FakeTransactionRepository()
        val tx = Transaction(
            id = "tx-1",
            description = "Freelance",
            amount = 50000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(tx)

        val useCase = GetTransactionByIdUseCase(repository)
        val result = useCase("tx-1")

        assertEquals(tx, result)
    }

    @Test
    fun `returns null when transaction does not exist in repository`() = runTest {
        val repository = FakeTransactionRepository()
        val useCase = GetTransactionByIdUseCase(repository)
        val result = useCase("unknown-id")

        assertNull(result)
    }
}
