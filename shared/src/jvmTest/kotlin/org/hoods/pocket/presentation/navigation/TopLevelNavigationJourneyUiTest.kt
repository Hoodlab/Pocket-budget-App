package org.hoods.pocket.presentation.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hoods.pocket.PocketBudgetRoot
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.di.startTestKoin
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TopLevelNavigationJourneyUiTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun compactLayout_bottomNavigation_persistsAcrossAllTopLevelScreens() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val authRepository = FakeAuthRepository(initialState = AuthState.Unauthenticated)

        startTestKoin(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            authRepository = authRepository
        )

        setContent {
            PocketBudgetRoot(
                layoutMode = AppLayoutMode.Compact
            )
        }

        waitForIdle()

        // 1. Initial screen: Transactions (bottom bar contains all 3 destinations)
        onNodeWithTag("add_transaction_fab").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 2. Navigate to Budgets
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        onNodeWithText("Monthly Budgets").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 3. Navigate to Account
        onNodeWithTag("nav_account").performClick()
        waitForIdle()

        onNodeWithTag("account_user_email").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 4. Return to Transactions
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()

        onNodeWithTag("add_transaction_fab").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()
    }

    @Test
    fun expandedLayout_sidebar_persistsAcrossAllTopLevelScreens() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()
        val authRepository = FakeAuthRepository(initialState = AuthState.Unauthenticated)

        startTestKoin(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            authRepository = authRepository
        )

        setContent {
            PocketBudgetRoot(
                layoutMode = AppLayoutMode.Expanded
            )
        }

        waitForIdle()

        // 1. Initial screen: Transactions (sidebar contains header + 3 destinations)
        onNodeWithText("Pocket Budget").assertIsDisplayed()
        onNodeWithText("Financial Wellness").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 2. Navigate to Budgets
        onNodeWithTag("nav_budgets").performClick()
        waitForIdle()

        onNodeWithText("Budget Progress").assertIsDisplayed()
        onNodeWithText("Pocket Budget").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 3. Navigate to Account
        onNodeWithTag("nav_account").performClick()
        waitForIdle()

        onNodeWithText("Pocket Budget").assertIsDisplayed()
        onNodeWithTag("account_user_email").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()

        // 4. Return to Transactions
        onNodeWithTag("nav_transactions").performClick()
        waitForIdle()

        onNodeWithText("Pocket Budget").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
        onNodeWithTag("nav_budgets").assertIsDisplayed()
        onNodeWithTag("nav_account").assertIsDisplayed()
    }

    @Test
    fun subScreen_addTransaction_operatesWithinNavHostAndReturnsToTransactions() = runComposeUiTest {
        val transactionRepository = FakeTransactionRepository()
        val budgetRepository = FakeBudgetRepository()

        startTestKoin(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository
        )

        setContent {
            PocketBudgetRoot(
                layoutMode = AppLayoutMode.Compact
            )
        }

        waitForIdle()

        // Tap FAB to open Add Transaction form
        onNodeWithTag("add_transaction_fab").performClick()
        waitForIdle()

        onNodeWithTag("description_input").assertIsDisplayed()
        onNodeWithTag("amount_input").assertIsDisplayed()

        // Dismiss returns to Transaction list
        onNodeWithTag("back_button").performClick()
        waitForIdle()

        onNodeWithTag("add_transaction_fab").assertIsDisplayed()
        onNodeWithTag("nav_transactions").assertIsDisplayed()
    }
}
