package org.hoods.pocket.presentation.budget.set

sealed interface SetBudgetEvent {
    data object SaveSuccess : SetBudgetEvent
    data object DeleteSuccess : SetBudgetEvent
}
