package org.hoods.pocket.domain.usecase

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.MIGRATION_2_3
import org.hoods.pocket.data.local.database.MIGRATION_3_4
import org.hoods.pocket.data.local.database.MIGRATION_4_5
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClaimLocalDataUseCaseTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomDataOwnershipRepository
    private lateinit var getSummaryUseCase: GetUnclaimedDataSummaryUseCase
    private lateinit var claimUseCase: ClaimLocalDataUseCase

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("claim_usecase_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        repository = RoomDataOwnershipRepository(database.dataOwnershipDao())
        getSummaryUseCase = GetUnclaimedDataSummaryUseCase(repository)
        claimUseCase = ClaimLocalDataUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun unclaimedSummary_returnsAccurateCountsAcrossAllTables() = runTest {
        // Initially empty
        val initialSummary = getSummaryUseCase()
        assertEquals(0, initialSummary.transactionCount)
        assertEquals(0, initialSummary.monthlyBudgetCount)
        assertEquals(0, initialSummary.categoryBudgetCount)
        assertFalse(initialSummary.hasUnclaimedData)

        // Seed 3 unassigned transactions, 1 monthly budget, 2 category budgets
        val txDao = database.transactionDao()
        txDao.insertTransaction(TransactionEntity("tx-1", "Lunch", 1500, "EXPENSE", 1000L, "DINING", null))
        txDao.insertTransaction(TransactionEntity("tx-2", "Bus", 250, "EXPENSE", 2000L, "TRANSPORT", null))
        txDao.insertTransaction(TransactionEntity("tx-3", "Gift", 5000, "INCOME", 3000L, "GIFT", null))

        val budgetDao = database.budgetDao()
        budgetDao.insertOrUpdateMonthlyBudget(MonthlyBudgetEntity(2026, 9, 300000L, null))

        val catDao = database.categoryBudgetDao()
        catDao.insertOrUpdateCategoryBudget(CategoryBudgetEntity(2026, 9, "DINING", 50000L, null))
        catDao.insertOrUpdateCategoryBudget(CategoryBudgetEntity(2026, 9, "TRANSPORT", 20000L, null))

        // Also seed an assigned record for another user
        txDao.insertTransaction(TransactionEntity("tx-other", "Other", 999, "EXPENSE", 4000L, "OTHER", "other_user"))

        val summary = getSummaryUseCase()
        assertEquals(3, summary.transactionCount)
        assertEquals(1, summary.monthlyBudgetCount)
        assertEquals(2, summary.categoryBudgetCount)
        assertEquals(6, summary.totalCount)
        assertTrue(summary.hasUnclaimedData)
    }

    @Test
    fun claimLocalData_atomicallyUpdatesAllTablesForUser() = runTest {
        val txDao = database.transactionDao()
        txDao.insertTransaction(TransactionEntity("tx-1", "Lunch", 1500, "EXPENSE", 1000L, "DINING", null))
        txDao.insertTransaction(TransactionEntity("tx-2", "Bus", 250, "EXPENSE", 2000L, "TRANSPORT", null))
        database.budgetDao().insertOrUpdateMonthlyBudget(MonthlyBudgetEntity(2026, 9, 300000L, null))
        database.categoryBudgetDao().insertOrUpdateCategoryBudget(CategoryBudgetEntity(2026, 9, "DINING", 50000L, null))

        // Claim for user_abc
        val result = claimUseCase("user_abc")
        assertTrue(result.isSuccess)

        // Summary must now be zero
        val summaryAfter = getSummaryUseCase()
        assertEquals(0, summaryAfter.totalCount)
        assertFalse(summaryAfter.hasUnclaimedData)

        // Verify that user_abc now sees all transactions
        val userTxs = txDao.getAllTransactions(activeUserId = "user_abc").first()
        assertEquals(2, userTxs.size)
        assertTrue(userTxs.all { it.userId == "user_abc" })
    }

    @Test
    fun claimLocalData_rejectsBlankUserId() = runTest {
        assertFailsWith<IllegalArgumentException> {
            claimUseCase("")
        }
        assertFailsWith<IllegalArgumentException> {
            claimUseCase("   ")
        }
    }

    @Test
    fun keepOnDevice_leavesAllRecordsUnassigned() = runTest {
        val txDao = database.transactionDao()
        txDao.insertTransaction(TransactionEntity("tx-keep", "Offline Item", 500, "EXPENSE", 1000L, "OTHER", null))

        // When user chooses "Keep on Device", no claim operation is performed
        val summary = getSummaryUseCase()
        assertEquals(1, summary.transactionCount)
        assertTrue(summary.hasUnclaimedData)

        // Records remain visible in unauthenticated mode
        val offlineTxs = txDao.getAllTransactions(activeUserId = null).first()
        assertEquals(1, offlineTxs.size)
        assertEquals("tx-keep", offlineTxs[0].id)
    }
}
