package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.hoods.pocket.data.repository.InMemoryBudgetRepository
import org.hoods.pocket.data.repository.InMemoryCategoryBudgetRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.usecase.DeleteMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetsUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase
import org.hoods.pocket.domain.usecase.RemoveCategoryBudgetUseCase
import org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCase
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import org.hoods.pocket.navigation.PocketNavHost
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModel
import org.hoods.pocket.presentation.budget.set.SetBudgetViewModel
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import org.hoods.pocket.presentation.transaction.add.AddTransactionViewModel
import org.hoods.pocket.presentation.transaction.edit.EditTransactionViewModel
import org.hoods.pocket.presentation.transaction.list.TransactionListViewModel

@Composable
fun App(
    repository: TransactionRepository,
    budgetRepository: BudgetRepository? = null,
    categoryBudgetRepository: CategoryBudgetRepository? = null
) {
    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            PocketBudgetRoot(
                repository = repository,
                budgetRepository = budgetRepository,
                categoryBudgetRepository = categoryBudgetRepository
            )
        }
    }
}

@Composable
fun PocketBudgetRoot(
    repository: TransactionRepository,
    budgetRepository: BudgetRepository? = null,
    categoryBudgetRepository: CategoryBudgetRepository? = null,
    modifier: Modifier = Modifier
) {
    val activeBudgetRepo = budgetRepository ?: remember { InMemoryBudgetRepository() }
    val activeCategoryBudgetRepo = categoryBudgetRepository ?: remember { InMemoryCategoryBudgetRepository() }

    val getTransactionsByMonthUseCase = remember(repository) { GetTransactionsByMonthUseCase(repository) }
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val updateTransactionUseCase = remember(repository) { UpdateTransactionUseCase(repository) }
    val deleteTransactionUseCase = remember(repository) { DeleteTransactionUseCase(repository) }
    val getTransactionByIdUseCase = remember(repository) { GetTransactionByIdUseCase(repository) }

    val setMonthlyBudgetUseCase = remember(activeBudgetRepo) { SetMonthlyBudgetUseCase(activeBudgetRepo) }
    val getMonthlyBudgetUseCase = remember(activeBudgetRepo) { GetMonthlyBudgetUseCase(activeBudgetRepo) }
    val getCategoryBudgetsUseCase = remember(activeCategoryBudgetRepo) {
        GetCategoryBudgetsUseCase(activeCategoryBudgetRepo)
    }
    val saveCategoryAllocationsUseCase = remember(activeBudgetRepo, activeCategoryBudgetRepo) {
        SaveCategoryAllocationsUseCase(activeBudgetRepo, activeCategoryBudgetRepo)
    }
    val removeCategoryBudgetUseCase = remember(activeCategoryBudgetRepo) {
        RemoveCategoryBudgetUseCase(activeCategoryBudgetRepo)
    }
    val deleteMonthlyBudgetUseCase = remember(activeBudgetRepo, activeCategoryBudgetRepo) {
        DeleteMonthlyBudgetUseCase(activeBudgetRepo, activeCategoryBudgetRepo)
    }
    val getMonthlyBudgetSummaryUseCase = remember(activeBudgetRepo, repository) {
        GetMonthlyBudgetSummaryUseCase(activeBudgetRepo, repository)
    }

    val listViewModel = remember(getTransactionsByMonthUseCase) {
        TransactionListViewModel(getTransactionsByMonthUseCase)
    }

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
        budgetDashboardViewModelFactory = { month ->
            BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = getMonthlyBudgetSummaryUseCase,
                initialMonth = month
            )
        },
        setBudgetViewModelFactory = { month ->
            SetBudgetViewModel(
                setMonthlyBudgetUseCase = setMonthlyBudgetUseCase,
                getMonthlyBudgetUseCase = getMonthlyBudgetUseCase,
                deleteMonthlyBudgetUseCase = deleteMonthlyBudgetUseCase,
                getCategoryBudgetsUseCase = getCategoryBudgetsUseCase,
                saveCategoryAllocationsUseCase = saveCategoryAllocationsUseCase,
                removeCategoryBudgetUseCase = removeCategoryBudgetUseCase,
                yearMonth = month
            )
        },
        modifier = modifier
    )
}