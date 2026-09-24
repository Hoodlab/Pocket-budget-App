package org.hoods.pocket.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.auth.account.AccountScreen
import org.hoods.pocket.presentation.auth.claim.ClaimDataScreen
import org.hoods.pocket.presentation.auth.setup.SetupConfigScreen
import org.hoods.pocket.presentation.auth.sign_in.SignInScreen
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardScreen
import org.hoods.pocket.presentation.budget.set.SetBudgetScreen
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.shell.PocketShellViewModel
import org.hoods.pocket.presentation.transaction.add.AddTransactionScreen
import org.hoods.pocket.presentation.transaction.edit.EditTransactionScreen
import org.hoods.pocket.presentation.transaction.list.TransactionListScreen

@Composable
fun PocketNavHost(
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    shellViewModel: PocketShellViewModel = koinViewModel()
) {
    val syncStatus by shellViewModel.syncStatus.collectAsState()
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
            val now = YearMonth.now()
            navController.navigate(
                Screen.BudgetDashboard(
                    year = now.year,
                    month = now.month
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
        modifier = modifier,
        syncStatus = syncStatus
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.TransactionList,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<Screen.TransactionList> {
                TransactionListScreen(
                    onAddTransactionClick = {
                        navController.navigate(Screen.AddTransaction)
                    },
                    onTransactionClick = { transactionId ->
                        navController.navigate(Screen.EditTransaction(transactionId))
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.AddTransaction> {
                AddTransactionScreen(
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
                EditTransactionScreen(
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
                BudgetDashboardScreen(
                    initialMonth = initialMonth,
                    onSetBudgetClick = { targetMonth ->
                        navController.navigate(
                            Screen.SetBudget(
                                year = targetMonth.year,
                                month = targetMonth.month
                            )
                        )
                    },
                    layoutMode = layoutMode
                )
            }
            composable<Screen.SetBudget> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.SetBudget>()
                val targetMonth = YearMonth(route.year, route.month)
                SetBudgetScreen(
                    targetMonth = targetMonth,
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
            composable<Screen.Auth> {
                SignInScreen(
                    onSignInSuccess = {
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

            composable<Screen.ClaimData> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.ClaimData>()
                ClaimDataScreen(
                    targetUserId = route.targetUserId,
                    onClaimFinished = {
                        navController.navigate(Screen.TransactionList) {
                            popUpTo<Screen.TransactionList> { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.Account> {
                AccountScreen(
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

            composable<Screen.SetupConfig> {
                SetupConfigScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
