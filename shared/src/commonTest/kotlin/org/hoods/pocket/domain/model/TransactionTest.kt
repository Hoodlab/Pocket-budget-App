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

    @Test
    fun `default category is UNCATEGORIZED`() {
        val transaction = Transaction(
            id = "tx-6",
            description = "Uncategorized Item",
            amount = 2500L,
            type = TransactionType.EXPENSE,
            date = testDate
        )
        assertEquals(TransactionCategory.UNCATEGORIZED, transaction.category)
    }

    @Test
    fun `valid category for expense is accepted`() {
        val transaction = Transaction(
            id = "tx-7",
            description = "Groceries",
            amount = 3500L,
            type = TransactionType.EXPENSE,
            date = testDate,
            category = TransactionCategory.GROCERIES
        )
        assertEquals(TransactionCategory.GROCERIES, transaction.category)
    }

    @Test
    fun `income-only category on expense is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-8",
                description = "Salary as Expense",
                amount = 500000L,
                type = TransactionType.EXPENSE,
                date = testDate,
                category = TransactionCategory.SALARY
            )
        }
    }

    @Test
    fun `expense-only category on income is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                id = "tx-9",
                description = "Groceries as Income",
                amount = 2000L,
                type = TransactionType.INCOME,
                date = testDate,
                category = TransactionCategory.GROCERIES
            )
        }
    }

    @Test
    fun `shared and uncategorized categories are accepted for both types`() {
        val expOther = Transaction(
            id = "tx-10",
            description = "Other Expense",
            amount = 1000L,
            type = TransactionType.EXPENSE,
            date = testDate,
            category = TransactionCategory.OTHER
        )
        val incOther = Transaction(
            id = "tx-11",
            description = "Other Income",
            amount = 1000L,
            type = TransactionType.INCOME,
            date = testDate,
            category = TransactionCategory.OTHER
        )
        val expUncat = Transaction(
            id = "tx-12",
            description = "Uncat Expense",
            amount = 1000L,
            type = TransactionType.EXPENSE,
            date = testDate,
            category = TransactionCategory.UNCATEGORIZED
        )
        val incUncat = Transaction(
            id = "tx-13",
            description = "Uncat Income",
            amount = 1000L,
            type = TransactionType.INCOME,
            date = testDate,
            category = TransactionCategory.UNCATEGORIZED
        )
        assertEquals(TransactionCategory.OTHER, expOther.category)
        assertEquals(TransactionCategory.OTHER, incOther.category)
        assertEquals(TransactionCategory.UNCATEGORIZED, expUncat.category)
        assertEquals(TransactionCategory.UNCATEGORIZED, incUncat.category)
    }
}
