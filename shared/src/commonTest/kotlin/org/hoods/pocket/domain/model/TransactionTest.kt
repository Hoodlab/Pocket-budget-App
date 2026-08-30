package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class TransactionTest {

    private val testDate = Instant.fromEpochMilliseconds(1717171717000L)

    @Test
    fun `positive amount is accepted`() {
        val transaction = Transaction(
            id = "tx-1",
            description = "Groceries",
            amount = 1500L,
            type = TransactionType.EXPENSE,
            date = testDate
        )

        assertEquals("tx-1", transaction.id)
        assertEquals("Groceries", transaction.description)
        assertEquals(1500L, transaction.amount)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(testDate, transaction.date)
    }

    @Test
    fun `zero amount is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-2",
                description = "Zero Amount",
                amount = 0L,
                type = TransactionType.INCOME,
                date = testDate
            )
        }
    }

    @Test
    fun `negative amount is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-3",
                description = "Negative Amount",
                amount = -500L,
                type = TransactionType.EXPENSE,
                date = testDate
            )
        }
    }

    @Test
    fun `blank description is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-4",
                description = "   ",
                amount = 1000L,
                type = TransactionType.EXPENSE,
                date = testDate
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-5",
                description = "",
                amount = 1000L,
                type = TransactionType.EXPENSE,
                date = testDate
            )
        }
    }
}
