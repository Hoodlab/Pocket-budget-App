package org.hoods.pocket

import androidx.compose.ui.window.ComposeUIViewController
import org.hoods.pocket.data.local.database.createBudgetRepository
import org.hoods.pocket.data.local.database.createCategoryBudgetRepository
import org.hoods.pocket.data.local.database.createTransactionRepository

fun MainViewController() = ComposeUIViewController {
    val repository = createTransactionRepository()
    val budgetRepository = createBudgetRepository()
    val categoryBudgetRepository = createCategoryBudgetRepository()
    App(
        repository = repository,
        budgetRepository = budgetRepository,
        categoryBudgetRepository = categoryBudgetRepository
    )
}