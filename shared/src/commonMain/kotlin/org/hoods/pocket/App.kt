package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.ui.navigation.Screen
import org.hoods.pocket.ui.theme.PocketColors
import org.hoods.pocket.ui.theme.PocketTheme
import org.hoods.pocket.ui.transaction.AddTransactionForm
import org.hoods.pocket.ui.transaction.TransactionListScreen

@Composable
fun App(
    repository: TransactionRepository
) {
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val coroutineScope = rememberCoroutineScope()
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())
    val navController = rememberNavController()

    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.TransactionList
            ) {
                composable<Screen.TransactionList> {
                    TransactionListScreen(
                        transactions = transactions,
                        onAddTransactionClick = {
                            navController.navigate(Screen.AddTransaction)
                        }
                    )
                }
                composable<Screen.AddTransaction> {
                    AddTransactionForm(
                        onSaveTransaction = { transaction ->
                            coroutineScope.launch {
                                addTransactionUseCase(transaction)
                                navController.popBackStack()
                            }
                        },
                        onDismiss = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}