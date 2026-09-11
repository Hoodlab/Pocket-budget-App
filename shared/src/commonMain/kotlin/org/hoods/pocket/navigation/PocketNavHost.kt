package org.hoods.pocket.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardScreen
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModel
import org.hoods.pocket.presentation.budget.set.SetBudgetScreen
import org.hoods.pocket.presentation.budget.set.SetBudgetViewModel
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
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TransactionList,
        modifier = modifier
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
                onNavigateToBudgets = {
                    val currentMonth = listViewModel.uiState.value.selectedMonth
                    navController.navigate(
                        Screen.BudgetDashboard(
                            year = currentMonth.year,
                            month = currentMonth.month
                        )
                    )
                }
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
                }
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
                }
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
                onNavigateToTransactions = {
                    navController.navigate(Screen.TransactionList) {
                        popUpTo<Screen.TransactionList> {
                            inclusive = false
                        }
                    }
                }
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
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}
