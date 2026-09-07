package org.hoods.pocket.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
    }
}
