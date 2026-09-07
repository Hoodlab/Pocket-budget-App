package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.repository.TransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AddTransactionUseCaseTest {

    @Test
    fun `adding a transaction saves it to repository`() = runTest {
        val repository = FakeTransactionRepository()
        val useCase = AddTransactionUseCase(repository)
        val transaction = Transaction(
            id = "tx-1",
            description = "Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )

        useCase(transaction)

        val savedTransactions = repository.getTransactions().first()
        assertEquals(listOf(transaction), savedTransactions)
    }
}
