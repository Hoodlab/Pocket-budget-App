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
}
