package org.hoods.pocket.presentation.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.PocketBudgetRoot
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.time.Instant

import org.hoods.pocket.domain.model.YearMonth

import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.CategoryBudget

@OptIn(ExperimentalTestApi::class)
class AuthJourneyUiTest {

    @Test
    fun completeAuthJourney_signIn_claimUnassignedData_viewAccount_andSignOut() = runComposeUiTest {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val authRepo = FakeAuthRepository()
        val dataOwnershipRepo = RoomDataOwnershipRepository(database.dataOwnershipDao())
        val transactionRepo = RoomTransactionRepository(
            dao = database.transactionDao(),
            authRepository = authRepo
        )
        val budgetRepo = RoomBudgetRepository(
            dao = database.budgetDao(),
            authRepository = authRepo
        )
        val categoryBudgetRepo = RoomCategoryBudgetRepository(
            dao = database.categoryBudgetDao(),
            authRepository = authRepo
        )

        val currentMonth = YearMonth.now()

        // 1. Seed unclaimed local records (when unauthenticated)
        runTest {
            transactionRepo.insertTransaction(
                Transaction(
                    id = "tx-local-1",
                    description = "Local Groceries",
                    amount = 4550L,
                    type = TransactionType.EXPENSE,
                    date = currentMonth.startInstant(),
                    category = TransactionCategory.GROCERIES
                )
            )
            budgetRepo.setMonthlyBudget(
                MonthlyBudget(
                    yearMonth = currentMonth,
                    limitAmount = 200000L
                )
            )
            categoryBudgetRepo.setCategoryBudget(
                CategoryBudget(
                    yearMonth = currentMonth,
                    category = TransactionCategory.GROCERIES,
                    limitAmount = 50000L
                )
            )
        }

        setContent {
            PocketBudgetRoot(
                repository = transactionRepo,
                budgetRepository = budgetRepo,
                categoryBudgetRepository = categoryBudgetRepo,
                authRepository = authRepo,
                dataOwnershipRepository = dataOwnershipRepo,
                supabaseConfig = SupabaseConfig.local()
            )
        }
        waitForIdle()

        // 2. Initial state (unauthenticated): Transaction List shows local transaction
        onNodeWithText("Local Groceries").assertIsDisplayed()

        // Verify unauthenticated Budgets dashboard shows local budget and category budget
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        onNodeWithText("2000.00", substring = true).assertIsDisplayed()
        onNodeWithText("Groceries", substring = true).assertIsDisplayed()

        // 3. Navigate to Account
        onNodeWithTag("nav_account").performClick()
        waitForIdle()

        // 4. Verify Account screen shows unauthenticated state
        onNodeWithText("Local Offline Account").assertIsDisplayed()
        onNodeWithText("Unauthenticated (Offline)").assertIsDisplayed()
        onNodeWithTag("sign_in_or_register_button").assertIsDisplayed()

        // 5. Click Sign In
        onNodeWithTag("sign_in_or_register_button").performClick()
        waitForIdle()

        // 6. On Sign In screen: enter credentials and submit
        onNodeWithTag("auth_email_input").performTextInput("testuser@example.com")
        onNodeWithTag("auth_password_input").performTextInput("password123")
        onNodeWithTag("auth_submit_button").performClick()
        waitForIdle()

        // 7. System detects unassigned local records -> navigates to Claim Data screen
        onNodeWithText("Claim Local Data").assertIsDisplayed()
        onNodeWithTag("claim_summary_card").assertIsDisplayed()
        onNodeWithTag("claim_for_account_button").assertIsDisplayed()

        // 8. Claim data for the account
        onNodeWithTag("claim_for_account_button").performClick()
        waitForIdle()

        // 9. Verify claimed data is visible in TransactionList
        onNodeWithText("Local Groceries").assertIsDisplayed()

        // Verify claimed budget and category budget are visible in Budgets dashboard
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        onNodeWithText("2000.00", substring = true).assertIsDisplayed()
        onNodeWithText("Groceries", substring = true).assertIsDisplayed()

        // 10. Navigate to Account screen to verify signed in user
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithText("testuser@example.com").assertIsDisplayed()
        onNodeWithText("Signed In").assertIsDisplayed()
        onNodeWithTag("sign_out_button").assertIsDisplayed()

        // 11. Sign Out
        onNodeWithTag("sign_out_button").performClick()
        waitForIdle()

        // 12. Returned to TransactionList in unauthenticated mode:
        // Claimed records belong to testuser@example.com, so they are NOT visible to unauthenticated session
        onNodeWithText("No transactions yet", substring = true).assertIsDisplayed()

        // Verify Budgets dashboard has no budget in unauthenticated session
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()
        onNodeWithText("No budget set for", substring = true).assertIsDisplayed()

        // 13. Revisit Account to verify unauthenticated
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithText("Local Offline Account").assertIsDisplayed()
        onNodeWithText("Unauthenticated (Offline)").assertIsDisplayed()

        database.close()
    }

    @Test
    fun keepOnDeviceJourney_leavesDataUnassigned_andVisibleToUnauthenticated() = runComposeUiTest {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val authRepo = FakeAuthRepository()
        val dataOwnershipRepo = RoomDataOwnershipRepository(database.dataOwnershipDao())
        val transactionRepo = RoomTransactionRepository(
            dao = database.transactionDao(),
            authRepository = authRepo
        )

        // Seed unassigned transaction
        runTest {
            transactionRepo.insertTransaction(
                Transaction(
                    id = "tx-local-keep",
                    description = "Local Coffee",
                    amount = 500L,
                    type = TransactionType.EXPENSE,
                    date = YearMonth.now().startInstant(),
                    category = TransactionCategory.DINING
                )
            )
        }

        setContent {
            PocketBudgetRoot(
                repository = transactionRepo,
                authRepository = authRepo,
                dataOwnershipRepository = dataOwnershipRepo,
                supabaseConfig = SupabaseConfig.local()
            )
        }
        waitForIdle()

        // Navigate to Account -> Sign In
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithTag("sign_in_or_register_button").performClick()
        waitForIdle()

        // Sign in
        onNodeWithTag("auth_email_input").performTextInput("other@example.com")
        onNodeWithTag("auth_password_input").performTextInput("password123")
        onNodeWithTag("auth_submit_button").performClick()
        waitForIdle()

        // On Claim screen, choose Keep on Device
        onNodeWithText("Claim Local Data").assertIsDisplayed()
        onNodeWithTag("keep_on_device_button").performClick()
        waitForIdle()

        // As signed-in user 'other@example.com', unassigned data is NOT scoped to this user
        onNodeWithText("No transactions yet", substring = true).assertIsDisplayed()

        // Sign out
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithTag("sign_out_button").performClick()
        waitForIdle()

        // Back in unauthenticated mode: unassigned transaction IS still visible!
        onNodeWithText("Local Coffee").assertIsDisplayed()

        database.close()
    }

    @Test
    fun setupConfigJourney_switchesRuntimeEnvironment() = runComposeUiTest {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val authRepo = FakeAuthRepository()
        val dataOwnershipRepo = RoomDataOwnershipRepository(database.dataOwnershipDao())
        val transactionRepo = RoomTransactionRepository(database.transactionDao(), authRepository = authRepo)

        val config = SupabaseConfig.local()

        setContent {
            PocketBudgetRoot(
                repository = transactionRepo,
                authRepository = authRepo,
                dataOwnershipRepository = dataOwnershipRepo,
                supabaseConfig = config
            )
        }
        waitForIdle()

        // Navigate to Account
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithTag("account_environment_label").assertTextContains("Local", substring = true)

        // Navigate to Setup
        onNodeWithTag("configure_environment_button").performClick()
        waitForIdle()
        onNodeWithText("Select Runtime Environment").assertIsDisplayed()

        // Select Hosted Dev
        onNodeWithTag("setup_select_hosted").performClick()
        waitForIdle()

        // Back on Account screen
        onNodeWithText("Account & Settings").assertIsDisplayed()

        database.close()
    }

    @Test
    fun signInErrorHandling_andModeToggling() = runComposeUiTest {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val authRepo = FakeAuthRepository(errorToReturn = "Invalid login credentials")
        val dataOwnershipRepo = RoomDataOwnershipRepository(database.dataOwnershipDao())
        val transactionRepo = RoomTransactionRepository(database.transactionDao(), authRepository = authRepo)

        setContent {
            PocketBudgetRoot(
                repository = transactionRepo,
                authRepository = authRepo,
                dataOwnershipRepository = dataOwnershipRepo,
                supabaseConfig = SupabaseConfig.local()
            )
        }
        waitForIdle()

        // Navigate to Account -> Sign In
        onNodeWithTag("nav_account").performClick()
        waitForIdle()
        onNodeWithTag("sign_in_or_register_button").performClick()
        waitForIdle()

        // Try to submit invalid credentials
        onNodeWithTag("auth_email_input").performTextInput("bad@example.com")
        onNodeWithTag("auth_password_input").performTextInput("wrongpass")
        onNodeWithTag("auth_submit_button").performClick()
        waitForIdle()

        // Verify error message displayed
        onNodeWithTag("auth_error_message").assertIsDisplayed()
        onNodeWithText("Invalid login credentials").assertIsDisplayed()

        // Toggle to Sign Up mode
        onNodeWithTag("auth_toggle_mode_button").performClick()
        waitForIdle()
        onNodeWithText("Create an Account").assertIsDisplayed()
        onNodeWithText("Create Account").assertIsDisplayed()

        // Toggle back to Sign In
        onNodeWithTag("auth_toggle_mode_button").performClick()
        waitForIdle()
        onNodeWithText("Welcome to Pocket Budget").assertIsDisplayed()

        database.close()
    }
}
