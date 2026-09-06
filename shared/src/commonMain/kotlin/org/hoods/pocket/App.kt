package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.ui.theme.PocketColors
import org.hoods.pocket.ui.theme.PocketTheme
import org.hoods.pocket.ui.transaction.AddTransactionForm
import org.hoods.pocket.ui.transaction.TransactionListScreen

enum class AppScreen {
    TRANSACTION_LIST,
    ADD_TRANSACTION
}

@Composable
fun App(
    repository: TransactionRepository
) {
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val coroutineScope = rememberCoroutineScope()
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())
    var currentScreen by remember { mutableStateOf(AppScreen.TRANSACTION_LIST) }

    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            when (currentScreen) {
                AppScreen.TRANSACTION_LIST -> {
                    TransactionListScreen(
                        transactions = transactions,
                        onAddTransactionClick = {
                            currentScreen = AppScreen.ADD_TRANSACTION
                        }
                    )
                }
                AppScreen.ADD_TRANSACTION -> {
                    AddTransactionForm(
                        onSaveTransaction = { transaction ->
                            coroutineScope.launch {
                                addTransactionUseCase(transaction)
                                currentScreen = AppScreen.TRANSACTION_LIST
                            }
                        },
                        onDismiss = {
                            currentScreen = AppScreen.TRANSACTION_LIST
                        }
                    )
                }
            }
        }
    }
}