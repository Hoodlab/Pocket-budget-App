package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoomDataOwnershipTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("data_ownership_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun unauthenticatedUser_seesOnlyUnclaimedRecords() = runTest {
        val txDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val catDao = database.categoryBudgetDao()

        // 1. Seed unclaimed records (userId = null)
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-unclaimed",
                description = "Local Cash Payment",
                amount = 1200L,
                type = "EXPENSE",
                dateEpochMillis = 1717171717000L,
                category = "OTHER",
                userId = null
            )
        )
        budgetDao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(
                year = 2026,
                month = 9,
                limitAmount = 200000L,
                userId = null
            )
        )
        catDao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(
                year = 2026,
                month = 9,
                category = "DINING",
                limitAmount = 40000L,
                userId = null
            )
        )

        // 2. Seed Account A records (userId = "user_a")
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-user-a",
                description = "Account A Flight",
                amount = 45000L,
                type = "EXPENSE",
                dateEpochMillis = 1717171718000L,
                category = "TRANSPORT",
                userId = "user_a"
            )
        )

        // 3. Seed Account B records (userId = "user_b")
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-user-b",
                description = "Account B Laptop",
                amount = 120000L,
                type = "EXPENSE",
                dateEpochMillis = 1717171719000L,
                category = "SHOPPING",
                userId = "user_b"
            )
        )

        // 4. Query as unauthenticated (activeUserId = null)
        val unauthTxs = txDao.getAllTransactions(activeUserId = null).first()
        assertEquals(1, unauthTxs.size)
        assertEquals("tx-unclaimed", unauthTxs[0].id)
        assertNull(unauthTxs[0].userId)

        val unauthBudget = budgetDao.getMonthlyBudget(2026, 9, activeUserId = null).first()
        assertNotNull(unauthBudget)
        assertNull(unauthBudget.userId)

        val unauthCatBudgets = catDao.getCategoryBudgets(2026, 9, activeUserId = null).first()
        assertEquals(1, unauthCatBudgets.size)
        assertNull(unauthCatBudgets[0].userId)
    }

    @Test
    fun authenticatedUser_seesOnlyOwnRecords() = runTest {
        val txDao = database.transactionDao()

        // Seed: unclaimed, Account A, and Account B transactions
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-offline",
                description = "Offline Coffee",
                amount = 350L,
                type = "EXPENSE",
                dateEpochMillis = 1717171717000L,
                userId = null
            )
        )
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-a",
                description = "User A Salary",
                amount = 500000L,
                type = "INCOME",
                dateEpochMillis = 1717171718000L,
                userId = "user_a"
            )
        )
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-b",
                description = "User B Groceries",
                amount = 6200L,
                type = "EXPENSE",
                dateEpochMillis = 1717171719000L,
                userId = "user_b"
            )
        )

        // Query as User A
        val userATxs = txDao.getAllTransactions(activeUserId = "user_a").first()
        assertEquals(1, userATxs.size)
        assertEquals("tx-a", userATxs[0].id)
        assertEquals("user_a", userATxs[0].userId)

        // Query as User B
        val userBTxs = txDao.getAllTransactions(activeUserId = "user_b").first()
        assertEquals(1, userBTxs.size)
        assertEquals("tx-b", userBTxs[0].id)
        assertEquals("user_b", userBTxs[0].userId)
    }

    @Test
    fun atomicClaiming_transfersAllUnassignedRecords_toTargetUser() = runTest {
        val txDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val catDao = database.categoryBudgetDao()
        val ownershipDao = database.dataOwnershipDao()

        // Seed 2 unassigned transactions, 1 monthly budget, 1 category budget
        txDao.insertTransaction(
            TransactionEntity(id = "tx-u1", description = "Tx 1", amount = 1000L, type = "EXPENSE", dateEpochMillis = 1717171717000L, userId = null)
        )
        txDao.insertTransaction(
            TransactionEntity(id = "tx-u2", description = "Tx 2", amount = 2000L, type = "INCOME", dateEpochMillis = 1717171718000L, userId = null)
        )
        budgetDao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(year = 2026, month = 9, limitAmount = 300000L, userId = null)
        )
        catDao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(year = 2026, month = 9, category = "GROCERIES", limitAmount = 50000L, userId = null)
        )

        // Also seed an Account B transaction that must NOT be touched
        txDao.insertTransaction(
            TransactionEntity(id = "tx-b-existing", description = "B Tx", amount = 9999L, type = "EXPENSE", dateEpochMillis = 1717171719000L, userId = "user_b")
        )

        assertEquals(2, ownershipDao.getUnclaimedTransactionCount())
        assertEquals(1, ownershipDao.getUnclaimedMonthlyBudgetCount())
        assertEquals(1, ownershipDao.getUnclaimedCategoryBudgetCount())

        // Atomically claim for user_a
        ownershipDao.claimAllUnassigned("user_a")

        // Unclaimed counts drop to 0
        assertEquals(0, ownershipDao.getUnclaimedTransactionCount())
        assertEquals(0, ownershipDao.getUnclaimedMonthlyBudgetCount())
        assertEquals(0, ownershipDao.getUnclaimedCategoryBudgetCount())

        // User A now sees the 2 claimed transactions
        val userATxs = txDao.getAllTransactions(activeUserId = "user_a").first()
        assertEquals(2, userATxs.size)

        // User B still has their single untouched transaction
        val userBTxs = txDao.getAllTransactions(activeUserId = "user_b").first()
        assertEquals(1, userBTxs.size)
        assertEquals("tx-b-existing", userBTxs[0].id)
    }

    @Test
    fun crossUserDeletion_isPreventedByActiveUserScoping() = runTest {
        val txDao = database.transactionDao()

        // User A owns tx-a
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-a-safe",
                description = "Secret A Tx",
                amount = 1500L,
                type = "EXPENSE",
                dateEpochMillis = 1717171717000L,
                userId = "user_a"
            )
        )

        // User B attempts to delete tx-a
        txDao.deleteTransactionById(id = "tx-a-safe", activeUserId = "user_b")

        // Transaction tx-a must still exist!
        val stillExists = txDao.getTransactionById(id = "tx-a-safe", activeUserId = "user_a")
        assertNotNull(stillExists)

        // User A successfully deletes their own transaction
        txDao.deleteTransactionById(id = "tx-a-safe", activeUserId = "user_a")
        val deleted = txDao.getTransactionById(id = "tx-a-safe", activeUserId = "user_a")
        assertNull(deleted)
    }
}
