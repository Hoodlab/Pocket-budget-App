package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.ui.theme.PocketColors
import org.hoods.pocket.ui.theme.PocketTheme
import org.hoods.pocket.ui.transaction.AddTransactionForm

@Composable
fun App(
    repository: TransactionRepository
) {
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val coroutineScope = rememberCoroutineScope()

    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            AddTransactionForm(
                onSaveTransaction = { transaction ->
                    coroutineScope.launch {
                        addTransactionUseCase(transaction)
                    }
                }
            )
        }
    }
}