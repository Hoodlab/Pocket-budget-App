package org.hoods.pocket.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RoomTransactionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomTransactionRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = RoomTransactionRepository(database.transactionDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTransaction_persistsAndEmitsInGetTransactions() = runTest {
        val transaction = Transaction(
            id = "tx-1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )

        repository.insertTransaction(transaction)

        val transactions = repository.getTransactions().first()
        assertEquals(listOf(transaction), transactions)
    }

    @Test
    fun getTransactions_emitsTransactionsOrderedByDateDescending() = runTest {
        val older = Transaction(
            id = "tx-1",
            description = "Older Salary",
            amount = 300000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1000L)
        )
        val newer = Transaction(
            id = "tx-2",
            description = "Newer Coffee",
            amount = 450L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(2000L)
        )

        repository.insertTransaction(older)
        repository.insertTransaction(newer)

        val transactions = repository.getTransactions().first()
        assertEquals(listOf(newer, older), transactions)
    }

    @Test
    fun insertTransaction_preservesLargeMonetaryValuesWithoutPrecisionLoss() = runTest {
        val largeAmountTransaction = Transaction(
            id = "tx-3",
            description = "Large Investment Return",
            amount = 9876543210123L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )

        repository.insertTransaction(largeAmountTransaction)

        val saved = repository.getTransactions().first().first()
        assertEquals(9876543210123L, saved.amount)
    }

    @Test
    fun persistsTransactionsAcrossDatabaseReopen() = runTest {
        val tempFile = java.io.File.createTempFile("pocket_budget_test", ".db")
        try {
            // First session: open database, insert transaction, close
            val db1 = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .build()
            val repo1 = RoomTransactionRepository(db1.transactionDao())

            val tx = Transaction(
                id = "tx-restart-1",
                description = "Salary",
                amount = 300000L,
                type = TransactionType.INCOME,
                date = Instant.fromEpochMilliseconds(1717171717000L)
            )
            repo1.insertTransaction(tx)
            db1.close()

            // Second session (simulating app restart): reopen database from same file
            val db2 = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .build()
            val repo2 = RoomTransactionRepository(db2.transactionDao())

            val recoveredTransactions = repo2.getTransactions().first()
            assertEquals(listOf(tx), recoveredTransactions)
            db2.close()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun insertTransaction_withExplicitCategory_persistsAndRetrievesCategory() = runTest {
        val transaction = Transaction(
            id = "tx-cat-1",
            description = "Trader Joe's",
            amount = 6750L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L),
            category = org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
        )

        repository.insertTransaction(transaction)

        val retrieved = repository.getTransactionById("tx-cat-1")
        assertEquals(transaction, retrieved)
        assertEquals(org.hoods.pocket.domain.model.TransactionCategory.GROCERIES, retrieved?.category)
    }

    @Test
    fun updateTransaction_modifiesCategoryInPlaceWithoutDuplication() = runTest {
        val original = Transaction(
            id = "tx-cat-2",
            description = "Dinner with friends",
            amount = 8900L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L),
            category = org.hoods.pocket.domain.model.TransactionCategory.UNCATEGORIZED
        )
        repository.insertTransaction(original)

        val updated = original.copy(category = org.hoods.pocket.domain.model.TransactionCategory.DINING)
        repository.updateTransaction(updated)

        val all = repository.getTransactions().first()
        assertEquals(1, all.size)
        assertEquals(org.hoods.pocket.domain.model.TransactionCategory.DINING, all.first().category)
    }

    @Test
    fun persistsCategoryAcrossDatabaseReopen() = runTest {
        val tempFile = java.io.File.createTempFile("pocket_budget_cat_restart", ".db")
        try {
            val db1 = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(org.hoods.pocket.data.local.database.MIGRATION_2_3)
                .build()
            val repo1 = RoomTransactionRepository(db1.transactionDao())

            val tx = Transaction(
                id = "tx-restart-cat",
                description = "Bus pass",
                amount = 2500L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(1717171717000L),
                category = org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT
            )
            repo1.insertTransaction(tx)
            db1.close()

            val db2 = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(org.hoods.pocket.data.local.database.MIGRATION_2_3)
                .build()
            val repo2 = RoomTransactionRepository(db2.transactionDao())

            val recovered = repo2.getTransactionById("tx-restart-cat")
            assertEquals(tx, recovered)
            assertEquals(org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT, recovered?.category)
            db2.close()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun updateTransaction_modifiesExistingRecordWithoutDuplication() = runTest {
        val original = Transaction(
            id = "tx-update-1",
            description = "Groceries",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(original)

        val updated = original.copy(
            description = "Supermarket",
            amount = 5200L,
            type = TransactionType.EXPENSE
        )
        repository.updateTransaction(updated)

        val transactions = repository.getTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(updated, transactions.first())
    }

    @Test
    fun deleteTransaction_removesRecordFromDatabase() = runTest {
        val tx1 = Transaction(
            id = "tx-del-1",
            description = "Coffee",
            amount = 450L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1000L)
        )
        val tx2 = Transaction(
            id = "tx-del-2",
            description = "Lunch",
            amount = 1200L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(2000L)
        )
        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        repository.deleteTransaction(tx1.id)

        val remaining = repository.getTransactions().first()
        assertEquals(listOf(tx2), remaining)
    }

    @Test
    fun getTransactionById_returnsCorrectTransactionOrNull() = runTest {
        val tx = Transaction(
            id = "tx-find-1",
            description = "Freelance",
            amount = 50000L,
            type = TransactionType.INCOME,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )
        repository.insertTransaction(tx)

        val found = repository.getTransactionById("tx-find-1")
        assertEquals(tx, found)

        val notFound = repository.getTransactionById("non-existent")
        assertEquals(null, notFound)
    }

    @Test
    fun getTransactionsByDateRange_returnsOnlyTransactionsInRangeOrderedDescending() = runTest {
        val startInstant = Instant.parse("2026-08-01T00:00:00Z")
        val endInstant = Instant.parse("2026-09-01T00:00:00Z")

        val txBefore = Transaction(
            id = "tx-before",
            description = "July Transaction",
            amount = 1000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-07-31T23:59:59Z")
        )
        val txStart = Transaction(
            id = "tx-start",
            description = "August Start Boundary",
            amount = 2000L,
            type = TransactionType.INCOME,
            date = Instant.parse("2026-08-01T00:00:00Z")
        )
        val txMid = Transaction(
            id = "tx-mid",
            description = "August Mid Month",
            amount = 3000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-08-15T12:00:00Z")
        )
        val txEnd = Transaction(
            id = "tx-end",
            description = "September Start Boundary",
            amount = 4000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-09-01T00:00:00Z")
        )
        val txAfter = Transaction(
            id = "tx-after",
            description = "September Mid Month",
            amount = 5000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-09-15T12:00:00Z")
        )

        repository.insertTransaction(txBefore)
        repository.insertTransaction(txStart)
        repository.insertTransaction(txMid)
        repository.insertTransaction(txEnd)
        repository.insertTransaction(txAfter)

        val result = repository.getTransactionsByDateRange(
            startEpochMillis = startInstant.toEpochMilliseconds(),
            endEpochMillis = endInstant.toEpochMilliseconds()
        ).first()

        assertEquals(listOf(txMid, txStart), result)
    }

    @Test
    fun getExpenseTotalByDateRange_sumsOnlyExpensesWithinRange_ignoringIncomeAndOutOfRange() = runTest {
        val startMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        val endMillis = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds()

        val txBeforeExpense = Transaction("1", "July Expense", 1000L, TransactionType.EXPENSE, Instant.parse("2026-07-31T23:59:59Z"))
        val txInExpense1 = Transaction("2", "Aug Expense 1", 3500L, TransactionType.EXPENSE, Instant.parse("2026-08-05T12:00:00Z"))
        val txInExpense2 = Transaction("3", "Aug Expense 2", 1500L, TransactionType.EXPENSE, Instant.parse("2026-08-20T12:00:00Z"))
        val txInIncome = Transaction("4", "Aug Income", 500000L, TransactionType.INCOME, Instant.parse("2026-08-15T12:00:00Z"))
        val txAfterExpense = Transaction("5", "Sep Expense", 4000L, TransactionType.EXPENSE, Instant.parse("2026-09-01T00:00:00Z"))

        repository.insertTransaction(txBeforeExpense)
        repository.insertTransaction(txInExpense1)
        repository.insertTransaction(txInExpense2)
        repository.insertTransaction(txInIncome)
        repository.insertTransaction(txAfterExpense)

        val total = repository.getExpenseTotalByDateRange(startMillis, endMillis).first()
        assertEquals(5000L, total) // 3500 + 1500; income and out-of-range excluded
    }

    @Test
    fun getCategorySpendingByDateRange_groupsExpensesByCategoryWithinRange_ignoringIncomeAndOutOfRange() = runTest {
        val startMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        val endMillis = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds()

        val txBeforeExpense = Transaction(
            "1", "July Expense", 1000L, TransactionType.EXPENSE,
            Instant.parse("2026-07-31T23:59:59Z"), org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
        )
        val txInExpense1 = Transaction(
            "2", "Aug Groceries 1", 2500L, TransactionType.EXPENSE,
            Instant.parse("2026-08-05T12:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
        )
        val txInExpense2 = Transaction(
            "3", "Aug Groceries 2", 1500L, TransactionType.EXPENSE,
            Instant.parse("2026-08-20T12:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
        )
        val txInDining = Transaction(
            "4", "Aug Dining", 3000L, TransactionType.EXPENSE,
            Instant.parse("2026-08-10T18:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.DINING
        )
        val txInIncome = Transaction(
            "5", "Aug Salary", 500000L, TransactionType.INCOME,
            Instant.parse("2026-08-15T12:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.SALARY
        )
        val txAfterExpense = Transaction(
            "6", "Sep Dining", 4000L, TransactionType.EXPENSE,
            Instant.parse("2026-09-01T00:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.DINING
        )

        repository.insertTransaction(txBeforeExpense)
        repository.insertTransaction(txInExpense1)
        repository.insertTransaction(txInExpense2)
        repository.insertTransaction(txInDining)
        repository.insertTransaction(txInIncome)
        repository.insertTransaction(txAfterExpense)

        val spending = repository.getCategorySpendingByDateRange(startMillis, endMillis).first()
        assertEquals(4000L, spending[org.hoods.pocket.domain.model.TransactionCategory.GROCERIES])
        assertEquals(3000L, spending[org.hoods.pocket.domain.model.TransactionCategory.DINING])
        assertEquals(null, spending[org.hoods.pocket.domain.model.TransactionCategory.SALARY])
    }

    @Test
    fun getCategorySpendingByDateRange_emptyTransactions_returnsEmptyMap() = runTest {
        val startMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        val endMillis = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds()

        val spending = repository.getCategorySpendingByDateRange(startMillis, endMillis).first()
        assertEquals(emptyMap(), spending)
    }

    @Test
    fun getIncomeTotalByDateRange_groupsIncomeWithinRange_ignoringExpenseAndOutOfRange() = runTest {
        val startMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        val endMillis = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds()

        val txBeforeIncome = Transaction(
            "1", "July Bonus", 100000L, TransactionType.INCOME,
            Instant.parse("2026-07-31T23:59:59Z"), org.hoods.pocket.domain.model.TransactionCategory.SALARY
        )
        val txInIncome1 = Transaction(
            "2", "Aug Salary", 450000L, TransactionType.INCOME,
            Instant.parse("2026-08-01T12:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.SALARY
        )
        val txInIncome2 = Transaction(
            "3", "Aug Freelance", 50000L, TransactionType.INCOME,
            Instant.parse("2026-08-15T12:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.FREELANCE
        )
        val txInExpense = Transaction(
            "4", "Aug Groceries", 258000L, TransactionType.EXPENSE,
            Instant.parse("2026-08-10T18:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
        )
        val txAfterIncome = Transaction(
            "5", "Sep Salary", 450000L, TransactionType.INCOME,
            Instant.parse("2026-09-01T00:00:00Z"), org.hoods.pocket.domain.model.TransactionCategory.SALARY
        )

        repository.insertTransaction(txBeforeIncome)
        repository.insertTransaction(txInIncome1)
        repository.insertTransaction(txInIncome2)
        repository.insertTransaction(txInExpense)
        repository.insertTransaction(txAfterIncome)

        val totalIncome = repository.getIncomeTotalByDateRange(startMillis, endMillis).first()
        assertEquals(500000L, totalIncome)
    }

    @Test
    fun getIncomeTotalByDateRange_emptyTransactions_returnsZero() = runTest {
        val startMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        val endMillis = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds()

        val totalIncome = repository.getIncomeTotalByDateRange(startMillis, endMillis).first()
        assertEquals(0L, totalIncome)
    }
}
