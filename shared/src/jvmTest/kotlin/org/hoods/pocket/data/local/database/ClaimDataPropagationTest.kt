package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaimDataPropagationTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private lateinit var authRepository: FakeAuthRepository

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("claim_propagation_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        authRepository = FakeAuthRepository(initialState = AuthState.Unauthenticated)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun traceIdentityPropagation_fromSignedOut_throughClaim_toSignedOut() = runTest {
        val currentMonth = YearMonth.now()
        val txDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val catDao = database.categoryBudgetDao()
        val ownershipDao = database.dataOwnershipDao()

        // 1. Seed unassigned local records (userId = null)
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx-local-1",
                description = "Unclaimed Coffee",
                amount = 500L,
                type = "EXPENSE",
                dateEpochMillis = currentMonth.startInstant().toEpochMilliseconds() + 1000L,
                category = "GROCERIES",
                userId = null
            )
        )
        budgetDao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(
                year = currentMonth.year,
                month = currentMonth.month,
                limitAmount = 100000L,
                userId = null
            )
        )
        catDao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(
                year = currentMonth.year,
                month = currentMonth.month,
                category = "GROCERIES",
                limitAmount = 20000L,
                userId = null
            )
        )

        // Wire repositories with reactive authRepository
        val txRepo = RoomTransactionRepository(
            dao = txDao,
            authRepository = authRepository
        )
        val budgetRepo = RoomBudgetRepository(
            dao = budgetDao,
            authRepository = authRepository
        )
        val catRepo = RoomCategoryBudgetRepository(
            dao = catDao,
            authRepository = authRepository
        )
        val ownershipRepo = RoomDataOwnershipRepository(ownershipDao)

        val getTransactionsByMonthUseCase = GetTransactionsByMonthUseCase(txRepo)
        val getCashFlowSummaryUseCase = GetCashFlowSummaryUseCase(txRepo)
        val getMonthlyBudgetSummaryUseCase = GetMonthlyBudgetSummaryUseCase(budgetRepo, txRepo)
        val getCategoryBudgetSummariesUseCase = GetCategoryBudgetSummariesUseCase(catRepo, txRepo)

        // 2. Unauthenticated: queries must see unassigned records
        val initialTxs = getTransactionsByMonthUseCase(currentMonth).first()
        assertEquals(1, initialTxs.size)
        assertEquals("Unclaimed Coffee", initialTxs.first().description)

        val initialBudget = getMonthlyBudgetSummaryUseCase(currentMonth).first()
        assertNotNull(initialBudget.budget)
        assertEquals(100000L, initialBudget.budget!!.limitAmount)
        assertEquals(500L, initialBudget.totalExpense)

        val initialCatSummaries = getCategoryBudgetSummariesUseCase(currentMonth).first()
        assertEquals(1, initialCatSummaries.size)
        assertEquals(500L, initialCatSummaries.first().spentAmount)

        val initialCashFlow = getCashFlowSummaryUseCase(currentMonth).first()
        assertEquals(500L, initialCashFlow.expense)

        // 3. Sign In
        val testUser = AuthUser(id = "user_456", email = "test@example.com")
        authRepository.setAuthenticated(testUser)

        // 4. Claim data for user_456
        ownershipRepo.claimLocalDataAtomically("user_456")

        // === VERIFY THE THREE THINGS SEPARATELY ===

        // Verification 1: The Room rows were updated with expected signed-in user ID
        val rawTx = txDao.getTransactionById("tx-local-1", "user_456")
        assertNotNull(rawTx)
        assertEquals("user_456", rawTx.userId)

        val rawBudget = budgetDao.getMonthlyBudget(currentMonth.year, currentMonth.month, "user_456").first()
        assertNotNull(rawBudget)
        assertEquals("user_456", rawBudget.userId)

        val rawCats = catDao.getCategoryBudgets(currentMonth.year, currentMonth.month, "user_456").first()
        assertEquals(1, rawCats.size)
        assertEquals("user_456", rawCats.first().userId)

        // Verification 2: Active authentication state contains that same user ID
        val currentAuthState = authRepository.authState.value
        assertTrue(currentAuthState is AuthState.Authenticated)
        assertEquals("user_456", currentAuthState.user.id)

        // Verification 3: Transaction, budget, category-budget, and dashboard queries receive that ID after claiming
        val claimedTxs = getTransactionsByMonthUseCase(currentMonth).first()
        assertEquals(1, claimedTxs.size, "Claimed transactions should appear for the signed-in account")
        assertEquals("Unclaimed Coffee", claimedTxs.first().description)

        val claimedBudget = getMonthlyBudgetSummaryUseCase(currentMonth).first()
        assertNotNull(claimedBudget.budget, "Claimed monthly budget should appear for the signed-in account")
        assertEquals(100000L, claimedBudget.budget!!.limitAmount)
        assertEquals(500L, claimedBudget.totalExpense)

        val claimedCatSummaries = getCategoryBudgetSummariesUseCase(currentMonth).first()
        assertEquals(1, claimedCatSummaries.size, "Claimed category budget summary should appear for signed-in account")
        assertEquals(500L, claimedCatSummaries.first().spentAmount)

        val claimedCashFlow = getCashFlowSummaryUseCase(currentMonth).first()
        assertEquals(500L, claimedCashFlow.expense)

        // 5. Sign Out: data should be hidden again
        authRepository.signOut()

        val signedOutTxs = getTransactionsByMonthUseCase(currentMonth).first()
        assertEquals(0, signedOutTxs.size, "Claimed transactions must be hidden after sign-out")

        val signedOutBudget = getMonthlyBudgetSummaryUseCase(currentMonth).first()
        assertNull(signedOutBudget.budget, "Claimed monthly budget must be hidden after sign-out")
        assertEquals(0L, signedOutBudget.totalExpense)

        val signedOutCatSummaries = getCategoryBudgetSummariesUseCase(currentMonth).first()
        assertEquals(0, signedOutCatSummaries.size, "Claimed category budget summaries must be hidden after sign-out")

        val signedOutCashFlow = getCashFlowSummaryUseCase(currentMonth).first()
        assertEquals(0L, signedOutCashFlow.expense)
    }
}
