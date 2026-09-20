package org.hoods.pocket.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.auth.account.AccountScreen
import org.hoods.pocket.presentation.auth.account.AccountViewModel
import org.hoods.pocket.presentation.auth.claim.ClaimDataScreen
import org.hoods.pocket.presentation.auth.claim.ClaimDataViewModel
import org.hoods.pocket.presentation.auth.setup.SetupConfigScreen
import org.hoods.pocket.presentation.auth.setup.SetupConfigViewModel
import org.hoods.pocket.presentation.auth.sign_in.SignInScreen
import org.hoods.pocket.presentation.auth.sign_in.SignInViewModel
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardScreen
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModel
import org.hoods.pocket.presentation.budget.set.SetBudgetScreen
import org.hoods.pocket.presentation.budget.set.SetBudgetViewModel
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.transaction.add.AddTransactionScreen
import org.hoods.pocket.presentation.transaction.add.AddTransactionViewModel
import org.hoods.pocket.presentation.transaction.edit.EditTransactionScreen
import org.hoods.pocket.presentation.transaction.edit.EditTransactionViewModel
import org.hoods.pocket.presentation.transaction.list.TransactionListScreen
import org.hoods.pocket.presentation.transaction.list.TransactionListViewModel

@Composable
fun PocketNavHost(
    listViewModel: TransactionListViewModel,
    addViewModelFactory: () -> AddTransactionViewModel,
    editViewModelFactory: () -> EditTransactionViewModel,
    budgetDashboardViewModelFactory: (YearMonth) -> BudgetDashboardViewModel,
    setBudgetViewModelFactory: (YearMonth) -> SetBudgetViewModel,
    signInViewModelFactory: (() -> SignInViewModel)? = null,
    claimDataViewModelFactory: ((String) -> ClaimDataViewModel)? = null,
    accountViewModelFactory: (() -> AccountViewModel)? = null,
    setupConfigViewModelFactory: (() -> SetupConfigViewModel)? = null,
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val activeDestination = when {
        currentRoute?.contains("TransactionList") == true ||
        currentRoute?.contains("AddTransaction") == true ||
        currentRoute?.contains("EditTransaction") == true -> TopLevelDestination.TRANSACTIONS
        currentRoute?.contains("Budget") == true -> TopLevelDestination.BUDGETS
        currentRoute?.contains("Account") == true ||
        currentRoute?.contains("Auth") == true ||
        currentRoute?.contains("ClaimData") == true ||
        currentRoute?.contains("SetupConfig") == true -> TopLevelDestination.ACCOUNT
        else -> TopLevelDestination.TRANSACTIONS
    }

    PocketNavigationShell(
        activeDestination = activeDestination,
        onNavigateToTransactions = {
            navController.navigate(Screen.TransactionList) {
                popUpTo<Screen.TransactionList> {
                    inclusive = false
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateToBudgets = {
            val currentMonth = listViewModel.uiState.value.selectedMonth
            navController.navigate(
                Screen.BudgetDashboard(
                    year = currentMonth.year,
                    month = currentMonth.month
                )
            ) {
                popUpTo<Screen.TransactionList> {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateToAccount = {
            navController.navigate(Screen.Account) {
                popUpTo<Screen.TransactionList> {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        layoutMode = layoutMode,
        modifier = modifier
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.TransactionList,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<Screen.TransactionList> {
                TransactionListScreen(
                    viewModel = listViewModel,
                    onAddTransactionClick = {
                        navController.navigate(Screen.AddTransaction)
                    },
                    onTransactionClick = { transactionId ->
                        navController.navigate(Screen.EditTransaction(transactionId))
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.AddTransaction> { backStackEntry ->
                val addViewModel = remember(backStackEntry) { addViewModelFactory() }
                AddTransactionScreen(
                    viewModel = addViewModel,
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.EditTransaction> { backStackEntry ->
                val editRoute = backStackEntry.toRoute<Screen.EditTransaction>()
                val editViewModel = remember(editRoute.transactionId) { editViewModelFactory() }
                EditTransactionScreen(
                    viewModel = editViewModel,
                    transactionId = editRoute.transactionId,
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onDeleteSuccess = {
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.BudgetDashboard> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.BudgetDashboard>()
                val initialMonth = if (route.year != null && route.month != null) {
                    YearMonth(route.year, route.month)
                } else {
                    YearMonth.now()
                }
                val dashboardViewModel = remember(route.year, route.month) {
                    budgetDashboardViewModelFactory(initialMonth)
                }
                BudgetDashboardScreen(
                    viewModel = dashboardViewModel,
                    onSetBudgetClick = {
                        val month = dashboardViewModel.uiState.value.selectedMonth
                        navController.navigate(
                            Screen.SetBudget(
                                year = month.year,
                                month = month.month
                            )
                        )
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.SetBudget> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.SetBudget>()
                val targetMonth = YearMonth(route.year, route.month)
                val setBudgetViewModel = remember(route.year, route.month) {
                    setBudgetViewModelFactory(targetMonth)
                }
                SetBudgetScreen(
                    viewModel = setBudgetViewModel,
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onDeleteSuccess = {
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    },
                    layoutMode = layoutMode
                )
            }

            // Auth Screens
            composable<Screen.Auth> { backStackEntry ->
                if (signInViewModelFactory != null) {
                    val signInViewModel = remember(backStackEntry) { signInViewModelFactory() }
                    SignInScreen(
                        viewModel = signInViewModel,
                        onSignInSuccess = { userId ->
                            navController.navigate(Screen.TransactionList) {
                                popUpTo<Screen.TransactionList> { inclusive = true }
                            }
                        },
                        onClaimRequired = { userId ->
                            navController.navigate(Screen.ClaimData(userId))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable<Screen.ClaimData> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.ClaimData>()
                if (claimDataViewModelFactory != null) {
                    val claimViewModel = remember(route.targetUserId) { claimDataViewModelFactory(route.targetUserId) }
                    ClaimDataScreen(
                        viewModel = claimViewModel,
                        onClaimFinished = {
                            navController.navigate(Screen.TransactionList) {
                                popUpTo<Screen.TransactionList> { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable<Screen.Account> { backStackEntry ->
                if (accountViewModelFactory != null) {
                    val accountViewModel = remember(backStackEntry) { accountViewModelFactory() }
                    AccountScreen(
                        viewModel = accountViewModel,
                        onSignOutSuccess = {
                            navController.navigate(Screen.TransactionList) {
                                popUpTo<Screen.TransactionList> { inclusive = true }
                            }
                        },
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth)
                        },
                        onNavigateToSetup = {
                            navController.navigate(Screen.SetupConfig)
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable<Screen.SetupConfig> { backStackEntry ->
                if (setupConfigViewModelFactory != null) {
                    val setupViewModel = remember(backStackEntry) { setupConfigViewModelFactory() }
                    SetupConfigScreen(
                        viewModel = setupViewModel,
                        onConfigChanged = {
                            navController.popBackStack()
                        },
                        onContinueOffline = {
                            navController.popBackStack()
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
