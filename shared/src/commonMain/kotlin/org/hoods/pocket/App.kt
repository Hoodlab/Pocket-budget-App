package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.GetTransactionsUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import org.hoods.pocket.navigation.PocketNavHost
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import org.hoods.pocket.presentation.transaction.add.AddTransactionViewModel
import org.hoods.pocket.presentation.transaction.edit.EditTransactionViewModel
import org.hoods.pocket.presentation.transaction.list.TransactionListViewModel

@Composable
fun App(
    repository: TransactionRepository
) {
    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            PocketBudgetRoot(repository = repository)
        }
    }
}

@Composable
fun PocketBudgetRoot(
    repository: TransactionRepository,
    modifier: Modifier = Modifier
) {
    val getTransactionsUseCase = remember(repository) { GetTransactionsUseCase(repository) }
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val updateTransactionUseCase = remember(repository) { UpdateTransactionUseCase(repository) }
    val deleteTransactionUseCase = remember(repository) { DeleteTransactionUseCase(repository) }
    val getTransactionByIdUseCase = remember(repository) { GetTransactionByIdUseCase(repository) }

    val listViewModel = remember(getTransactionsUseCase) { TransactionListViewModel(getTransactionsUseCase) }

    PocketNavHost(
        listViewModel = listViewModel,
        addViewModelFactory = { AddTransactionViewModel(addTransactionUseCase) },
        editViewModelFactory = {
            EditTransactionViewModel(
                getTransactionByIdUseCase = getTransactionByIdUseCase,
                updateTransactionUseCase = updateTransactionUseCase,
                deleteTransactionUseCase = deleteTransactionUseCase
            )
        },
        modifier = modifier
    )
}